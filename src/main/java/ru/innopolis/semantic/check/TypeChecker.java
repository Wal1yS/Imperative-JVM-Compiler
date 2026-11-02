package ru.innopolis.semantic.check;

import ru.innopolis.ast.ASTNode;
import ru.innopolis.ast.Declaration;
import ru.innopolis.ast.ProgramNode;
import ru.innopolis.ast.declaration.*;
import ru.innopolis.ast.expression.*;
import ru.innopolis.ast.statement.*;
import ru.innopolis.semantic.context.SemanticContext;
import ru.innopolis.semantic.scope.IScope;
import ru.innopolis.semantic.symbol.*;
import ru.innopolis.semantic.type.*;
import ru.innopolis.semantic.visitor.BaseASTVisitor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static ru.innopolis.semantic.type.PrimitiveType.*;

public class TypeChecker extends BaseASTVisitor<Type, IScope> {
    private final SemanticContext context;
    private final TypeResolver resolver;
    private final Map<RoutineSymbol, RoutineDeclarationNode> forwardDeclarations = new HashMap<>();

    private Type currentReturnType;
    private String currentRoutineName;

    public TypeChecker(SemanticContext context) {
        this.context = context;
        this.resolver = new TypeResolver(context, this::valueType);
    }

    @Override
    public Type visit(ProgramNode node, IScope ignored) {
        IScope global = context.getGlobalScope();
        for (Declaration decl : node.getDeclarations()) {
            decl.accept(this, global);
        }
        return null;
    }

    @Override
    public Type visit(TypeDeclarationNode node, IScope scope) {
        if (context.getDeclaredSymbol(node) instanceof TypeSymbol typeSym) {
            resolver.resolveSymbol(typeSym, scope);
        } else {
            resolver.resolve(node.getType(), scope);
        }
        return null;
    }

    @Override
    public Type visit(VariableDeclarationNode node, IScope scope) {
        Type declared = null;
        if (node.getType() != null) {
            declared = resolver.resolve(node.getType(), scope);
            if (declared instanceof ArrayType array && !array.isSized()) {
                context.addError(node.getPosition(), "Variable '" + node.getName()
                        + "' needs an array size: sizeless 'array T' is allowed only for parameters and return types");
            }
        }

        Type init = node.getInitializer() != null ? valueType(node.getInitializer(), scope) : null;
        if (declared != null && init != null && !TypeSystem.isAssignable(declared, init)) {
            context.addError(node.getInitializer().getPosition(), "Cannot initialize variable '" + node.getName()
                    + "' of type " + declared + " with a value of type " + init);
        }

        Type varType = declared != null ? declared : (init != null ? init : ErrorType.INSTANCE);
        Symbol sym = context.getDeclaredSymbol(node);
        if (sym != null) {
            context.putSymbolType(sym, varType);
        }
        return null;
    }

    @Override
    public Type visit(RoutineDeclarationNode node, IScope scope) {
        RoutineSymbol routine = context.getDeclaredSymbol(node) instanceof RoutineSymbol r ? r : null;
        IScope routineScope = context.getNodeScope(node) != null ? context.getNodeScope(node) : scope;

        List<Type> paramTypes = node.getParameters().stream()
                .map(p -> resolver.resolve(p.getType(), scope))
                .toList();
        Type returnType = resolver.resolve(node.getReturnType(), scope);

        if (node.isForwardDeclaration()) {
            if (routine != null) forwardDeclarations.put(routine, node);
            return null;
        }

        for (int i = 0; i < node.getParameters().size(); i++) {
            Symbol paramSym = context.getDeclaredSymbol(node.getParameters().get(i));
            if (paramSym != null) context.putSymbolType(paramSym, paramTypes.get(i));
        }

        RoutineDeclarationNode forward = routine != null ? forwardDeclarations.get(routine) : null;
        if (forward != null) {
            checkForwardSignature(node, forward, paramTypes, returnType, scope);
        }

        Type savedReturn = currentReturnType;
        String savedName = currentRoutineName;
        currentReturnType = returnType;
        currentRoutineName = node.getName();

        if (node.getBody() != null) {
            node.getBody().accept(this, routineScope);
            if (returnType != VoidType.INSTANCE && !definitelyReturns(node.getBody())) {
                context.addError(node.getPosition(), "Routine '" + node.getName()
                        + "' must return a value of type " + returnType + " on every path");
            }
        }
        if (node.getExpressionBody() != null) {
            Type bodyType = valueType(node.getExpressionBody(), routineScope);
            if (returnType == VoidType.INSTANCE) {
                context.addError(node.getPosition(), "Routine '" + node.getName()
                        + "' has an expression body '=>' but no return type");
            } else if (!TypeSystem.isAssignable(returnType, bodyType)) {
                context.addError(node.getExpressionBody().getPosition(), "Routine '" + node.getName()
                        + "' must return " + returnType + ", but its expression body has type " + bodyType);
            }
        }

        currentReturnType = savedReturn;
        currentRoutineName = savedName;
        return null;
    }

    private void checkForwardSignature(RoutineDeclarationNode full, RoutineDeclarationNode forward,
                                       List<Type> paramTypes, Type returnType, IScope scope) {
        if (forward.getParameters().size() != full.getParameters().size()) return;
        for (int i = 0; i < paramTypes.size(); i++) {
            Type expected = resolver.resolve(forward.getParameters().get(i).getType(), scope);
            if (!TypeSystem.sameType(expected, paramTypes.get(i))) {
                context.addError(full.getParameters().get(i).getPosition(), "Parameter " + (i + 1) + " of routine '"
                        + full.getName() + "' has type " + paramTypes.get(i)
                        + ", but its forward declaration says " + expected);
            }
        }
        Type expectedReturn = resolver.resolve(forward.getReturnType(), scope);
        if (!TypeSystem.sameType(expectedReturn, returnType)) {
            context.addError(full.getPosition(), "Routine '" + full.getName() + "' returns " + returnType
                    + ", but its forward declaration says " + expectedReturn);
        }
    }

    private static boolean definitelyReturns(BodyNode body) {
        for (ASTNode item : body.getItems()) {
            if (item instanceof ReturnStatementNode) return true;
            if (item instanceof IfStatementNode ifs && ifs.getElseBody() != null
                    && definitelyReturns(ifs.getThenBody()) && definitelyReturns(ifs.getElseBody())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Type visit(BodyNode node, IScope scope) {
        IScope bodyScope = context.getNodeScope(node) != null ? context.getNodeScope(node) : scope;
        for (ASTNode item : node.getItems()) {
            item.accept(this, bodyScope);
        }
        return null;
    }

    @Override
    public Type visit(AssignmentNode node, IScope scope) {
        Type target = valueType(node.getTarget(), scope);
        Type value = valueType(node.getValue(), scope);

        if (node.getTarget() instanceof RecordAccessNode access && access.getFieldName().equals("size")
                && context.getExpressionType(access.getRecord()) instanceof ArrayType) {
            context.addError(node.getTarget().getPosition(), "Cannot assign to read-only property 'size' of an array");
            return null;
        }
        if (node.getTarget() instanceof VariableReferenceNode ref
                && !(context.getResolvedSymbol(ref) instanceof VariableSymbol
                || context.getResolvedSymbol(ref) instanceof ParameterSymbol)) {
            return null;
        }
        if (!TypeSystem.isAssignable(target, value)) {
            context.addError(node.getPosition(), "Type mismatch in assignment: cannot assign " + value + " to " + target);
        }
        return null;
    }

    @Override
    public Type visit(IfStatementNode node, IScope scope) {
        checkCondition(node.getCondition(), scope, "if");
        node.getThenBody().accept(this, scope);
        if (node.getElseBody() != null) {
            node.getElseBody().accept(this, scope);
        }
        return null;
    }

    @Override
    public Type visit(WhileLoopNode node, IScope scope) {
        checkCondition(node.getCondition(), scope, "while");
        node.getBody().accept(this, scope);
        return null;
    }

    private void checkCondition(Expression condition, IScope scope, String keyword) {
        Type type = valueType(condition, scope);
        if (!type.isError() && type != BOOLEAN) {
            context.addError(condition.getPosition(), "Condition of '" + keyword + "' must be boolean, got " + type);
        }
    }

    @Override
    public Type visit(ForLoopNode node, IScope scope) {
        RangeNode range = node.getRange();
        Type loopVarType;
        if (range.isArrayIteration()) {
            Type iterated = valueType(range.getStart(), scope);
            if (iterated instanceof ArrayType array) {
                loopVarType = array.getElementType();
            } else {
                if (!iterated.isError()) {
                    context.addError(range.getStart().getPosition(),
                            "'for " + node.getVariableName() + " in' without '..' requires an array, got " + iterated);
                }
                loopVarType = ErrorType.INSTANCE;
            }
        } else {
            checkRangeBound(range.getStart(), scope);
            checkRangeBound(range.getEnd(), scope);
            loopVarType = INTEGER;
        }

        Symbol loopVar = context.getDeclaredSymbol(node);
        if (loopVar != null) {
            context.putSymbolType(loopVar, loopVarType);
        }
        node.getBody().accept(this, scope);
        return null;
    }

    private void checkRangeBound(Expression bound, IScope scope) {
        Type type = valueType(bound, scope);
        if (!type.isError() && type != INTEGER) {
            context.addError(bound.getPosition(), "Range bounds of 'for' must be integer, got " + type);
        }
    }

    @Override
    public Type visit(PrintStatementNode node, IScope scope) {
        for (Expression expr : node.getExpressions()) {
            Type type = valueType(expr, scope);
            if (!TypeSystem.isPrintable(type)) {
                context.addError(expr.getPosition(), "Cannot print a value of type " + type
                        + ": only integer, real, boolean and string can be printed");
            }
        }
        return null;
    }

    @Override
    public Type visit(ReturnStatementNode node, IScope scope) {
        Type value = node.getValue() != null ? valueType(node.getValue(), scope) : null;
        if (currentReturnType == null) {
            return null;
        }
        if (currentReturnType == VoidType.INSTANCE) {
            if (value != null) {
                context.addError(node.getPosition(), "Routine '" + currentRoutineName
                        + "' has no return type and cannot return a value");
            }
        } else if (value == null) {
            context.addError(node.getPosition(), "Routine '" + currentRoutineName
                    + "' must return a value of type " + currentReturnType);
        } else if (!TypeSystem.isAssignable(currentReturnType, value)) {
            context.addError(node.getValue().getPosition(), "Return type mismatch in routine '" + currentRoutineName
                    + "': expected " + currentReturnType + ", got " + value);
        }
        return null;
    }

    @Override
    public Type visit(RoutineCallStatementNode node, IScope scope) {
        typeOf(node.getCall(), scope);
        return null;
    }

    private Type valueType(Expression expr, IScope scope) {
        Type type = typeOf(expr, scope);
        if (type == VoidType.INSTANCE) {
            String name = expr instanceof RoutineCallExpressionNode call ? call.getRoutineName() : "expression";
            context.addError(expr.getPosition(), "Routine '" + name
                    + "' has no return type, so its call cannot be used as a value");
            type = ErrorType.INSTANCE;
            context.putExpressionType(expr, type);
        }
        return type;
    }

    private Type typeOf(Expression expr, IScope scope) {
        Type type = expr.accept(this, scope);
        if (type == null) type = ErrorType.INSTANCE;
        context.putExpressionType(expr, type);
        return type;
    }

    @Override
    public Type visit(IntLiteralNode node, IScope scope) {
        return INTEGER;
    }

    @Override
    public Type visit(RealLiteralNode node, IScope scope) {
        return REAL;
    }

    @Override
    public Type visit(BoolLiteralNode node, IScope scope) {
        return BOOLEAN;
    }

    @Override
    public Type visit(StringLiteralNode node, IScope scope) {
        return STRING;
    }

    @Override
    public Type visit(VariableReferenceNode node, IScope scope) {
        Symbol sym = context.getResolvedSymbol(node);
        if (sym == null) {
            return ErrorType.INSTANCE;
        }
        if (sym instanceof RoutineSymbol) {
            context.addError(node.getPosition(), "'" + node.getName() + "' is a routine, not a variable; call it as "
                    + node.getName() + "(...)");
            return ErrorType.INSTANCE;
        }
        if (sym instanceof TypeSymbol) {
            context.addError(node.getPosition(), "'" + node.getName() + "' is a type, not a variable");
            return ErrorType.INSTANCE;
        }
        Type type = context.getSymbolType(sym);
        if (type == null && sym instanceof ParameterSymbol param) {
            type = resolver.resolve(param.getTypeNode(), context.getGlobalScope());
            context.putSymbolType(sym, type);
        }
        return type != null ? type : ErrorType.INSTANCE;
    }

    @Override
    public Type visit(BinaryExpressionNode node, IScope scope) {
        Type left = valueType(node.getLeft(), scope);
        Type right = valueType(node.getRight(), scope);
        Type result = TypeSystem.binaryResult(node.getOperator(), left, right);
        if (result == null) {
            context.addError(node.getPosition(), "Operator '" + TypeSystem.operatorSymbol(node.getOperator())
                    + "' cannot be applied to " + left + " and " + right);
            return ErrorType.INSTANCE;
        }
        return result;
    }

    @Override
    public Type visit(UnaryExpressionNode node, IScope scope) {
        Type operand = valueType(node.getOperand(), scope);
        Type result = TypeSystem.unaryResult(node.getOperator(), operand);
        if (result == null) {
            context.addError(node.getPosition(), "Operator '" + TypeSystem.operatorSymbol(node.getOperator())
                    + "' cannot be applied to " + operand);
            return ErrorType.INSTANCE;
        }
        return result;
    }

    @Override
    public Type visit(CastExpressionNode node, IScope scope) {
        Type source = valueType(node.getExpression(), scope);
        Type target = resolver.resolve(node.getTargetType(), scope);
        if (!target.isError() && !(target instanceof PrimitiveType)) {
            context.addError(node.getPosition(), "'as' converts only to primitive types, got " + target);
            return ErrorType.INSTANCE;
        }
        if (!TypeSystem.canCast(source, target)) {
            context.addError(node.getPosition(), "Cannot convert " + source + " to " + target + " with 'as'");
            return ErrorType.INSTANCE;
        }
        return target;
    }

    @Override
    public Type visit(ArrayAccessNode node, IScope scope) {
        Type array = valueType(node.getArray(), scope);
        Type index = valueType(node.getIndex(), scope);
        if (!index.isError() && index != INTEGER) {
            context.addError(node.getIndex().getPosition(), "Array index must be integer, got " + index);
        }
        if (array instanceof ArrayType arrayType) {
            return arrayType.getElementType();
        }
        if (!array.isError()) {
            context.addError(node.getPosition(), "Cannot index a value of type " + array + ": it is not an array");
        }
        return ErrorType.INSTANCE;
    }

    @Override
    public Type visit(RecordAccessNode node, IScope scope) {
        Type owner = valueType(node.getRecord(), scope);
        String field = node.getFieldName();
        if (owner instanceof ArrayType) {
            if (field.equals("size")) {
                return INTEGER;
            }
            context.addError(node.getPosition(), "Arrays have no member '" + field + "' (only 'size')");
            return ErrorType.INSTANCE;
        }
        if (owner instanceof RecordType record) {
            Type fieldType = record.getField(field);
            if (fieldType == null) {
                context.addError(node.getPosition(), "Record type '" + record + "' has no field '" + field + "'");
                return ErrorType.INSTANCE;
            }
            return fieldType;
        }
        if (!owner.isError()) {
            context.addError(node.getPosition(), "Cannot access member '" + field + "' of a value of type " + owner);
        }
        return ErrorType.INSTANCE;
    }

    @Override
    public Type visit(RoutineCallExpressionNode node, IScope scope) {
        RoutineSymbol routine = context.getResolvedRoutine(node);
        List<Expression> args = node.getArguments();
        if (routine == null) {
            for (Expression arg : args) valueType(arg, scope);
            return ErrorType.INSTANCE;
        }

        IScope global = context.getGlobalScope();
        List<ParameterSymbol> params = routine.getParameters();
        if (args.size() != params.size()) {
            context.addError(node.getPosition(), "Routine '" + routine.getName() + "' expects " + params.size()
                    + " argument(s), but " + args.size() + " were given");
        }
        for (int i = 0; i < args.size(); i++) {
            Type argType = valueType(args.get(i), scope);
            if (i >= params.size()) continue;
            Type paramType = resolver.resolve(params.get(i).getTypeNode(), global);
            if (!TypeSystem.isAssignable(paramType, argType)) {
                context.addError(args.get(i).getPosition(), "Argument " + (i + 1) + " of routine '" + routine.getName()
                        + "': cannot pass " + argType + " as " + paramType);
            }
        }
        return resolver.resolve(routine.getReturnTypeNode(), global);
    }
}
