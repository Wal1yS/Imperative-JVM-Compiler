package ru.innopolis.semantic.visitor;

import ru.innopolis.ast.*;
import ru.innopolis.ast.declaration.*;
import ru.innopolis.ast.expression.*;
import ru.innopolis.ast.statement.*;
import ru.innopolis.ast.type.*;
import ru.innopolis.semantic.context.SemanticContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Rewrites an immutable, successfully analyzed AST and preserves annotations of replacements.
 * Unchanged nodes keep their identity. Only elements of a BodyNode may be removed or replaced
 * with another kind of item (in particular, an if may become a scoped BodyNode).
 */
public class ASTTransformer implements ASTVisitor<ASTNode, Void> {
    protected final SemanticContext context;

    public ASTTransformer(SemanticContext context) {
        this.context = Objects.requireNonNull(context, "Semantic context is required");
    }

    public ASTNode transform(ASTNode node) {
        if (node == null) return null;
        if (node instanceof ProgramNode && (context.hasErrors() || context.getGlobalScope() == null
                || context.getNodeScope(node) != context.getGlobalScope())) {
            throw new IllegalArgumentException("Transformations require a successfully analyzed program and its context");
        }
        ASTNode result = node.accept(this, null);
        if (result != null && result != node) copyAnnotations(node, result);
        return result;
    }

    public ProgramNode transformProgram(ProgramNode node) {
        return required(Objects.requireNonNull(node, "Program is required"), ProgramNode.class);
    }

    public BodyNode transformBody(BodyNode node) {
        return required(node, BodyNode.class);
    }

    public Expression transformExpression(Expression node) {
        return required(node, Expression.class);
    }

    public TypeNode transformType(TypeNode node) {
        return required(node, TypeNode.class);
    }

    private <T extends ASTNode> T required(T node, Class<T> kind) {
        if (node == null) return null;
        return kind.cast(Objects.requireNonNull(transform(node), "Cannot remove a required child node"));
    }

    private <T extends ASTNode> List<T> rewriteList(List<T> nodes, Class<T> kind) {
        List<T> rewritten = new ArrayList<>(nodes.size());
        boolean changed = false;
        for (T node : nodes) {
            T replacement = required(node, kind);
            rewritten.add(replacement);
            changed |= replacement != node;
        }
        return changed ? rewritten : nodes;
    }

    /** Replacing an if by its body must retain the body's scope, not the scope of the if. */
    protected void copyAnnotations(ASTNode original, ASTNode replacement) {
        if (original.getClass() == replacement.getClass()) {
            var scope = context.getNodeScope(original);
            if (scope != null) context.putNodeScope(replacement, scope);
            var symbol = context.getDeclaredSymbol(original);
            if (symbol != null) context.putDeclaredSymbol(replacement, symbol);
        }
        if (original instanceof Expression oldExpr && replacement instanceof Expression newExpr) {
            var type = context.getExpressionType(oldExpr);
            if (type != null) context.putExpressionType(newExpr, type);
        }
        if (original instanceof VariableReferenceNode oldRef && replacement instanceof VariableReferenceNode newRef) {
            var symbol = context.getResolvedSymbol(oldRef);
            if (symbol != null) context.putResolvedSymbol(newRef, symbol);
        }
        if (original instanceof RoutineCallExpressionNode oldCall && replacement instanceof RoutineCallExpressionNode newCall) {
            var routine = context.getResolvedRoutine(oldCall);
            if (routine != null) context.putResolvedRoutine(newCall, routine);
        }
    }

    @Override
    public ASTNode visit(ProgramNode node, Void unused) {
        List<Declaration> declarations = rewriteList(node.getDeclarations(), Declaration.class);
        return declarations == node.getDeclarations() ? node : new ProgramNode(node.getPosition(), declarations);
    }

    @Override
    public ASTNode visit(RoutineDeclarationNode node, Void unused) {
        List<ParameterNode> parameters = rewriteList(node.getParameters(), ParameterNode.class);
        TypeNode result = transformType(node.getReturnType());
        BodyNode body = transformBody(node.getBody());
        Expression expression = transformExpression(node.getExpressionBody());
        if (parameters == node.getParameters() && result == node.getReturnType()
                && body == node.getBody() && expression == node.getExpressionBody()) return node;
        return new RoutineDeclarationNode(node.getPosition(), node.getName(), parameters, result, body, expression);
    }

    @Override
    public ASTNode visit(VariableDeclarationNode node, Void unused) {
        TypeNode type = transformType(node.getType());
        Expression initializer = transformExpression(node.getInitializer());
        return type == node.getType() && initializer == node.getInitializer() ? node
                : new VariableDeclarationNode(node.getPosition(), node.getName(), type, initializer);
    }

    @Override
    public ASTNode visit(TypeDeclarationNode node, Void unused) {
        TypeNode type = transformType(node.getType());
        return type == node.getType() ? node : new TypeDeclarationNode(node.getPosition(), node.getName(), type);
    }

    @Override
    public ASTNode visit(ParameterNode node, Void unused) {
        TypeNode type = transformType(node.getType());
        return type == node.getType() ? node : new ParameterNode(node.getPosition(), node.getName(), type);
    }

    @Override
    public ASTNode visit(BodyNode node, Void unused) {
        List<ASTNode> items = new ArrayList<>();
        boolean changed = false;
        for (ASTNode item : node.getItems()) {
            ASTNode replacement = transform(item);
            changed |= replacement != item;
            if (replacement != null) items.add(replacement);
        }
        return changed ? new BodyNode(node.getPosition(), items) : node;
    }

    @Override
    public ASTNode visit(AssignmentNode node, Void unused) {
        Expression target = transformExpression(node.getTarget());
        Expression value = transformExpression(node.getValue());
        return target == node.getTarget() && value == node.getValue() ? node : new AssignmentNode(target, value);
    }

    @Override
    public ASTNode visit(IfStatementNode node, Void unused) {
        Expression condition = transformExpression(node.getCondition());
        BodyNode thenBody = transformBody(node.getThenBody());
        BodyNode elseBody = transformBody(node.getElseBody());
        return condition == node.getCondition() && thenBody == node.getThenBody() && elseBody == node.getElseBody()
                ? node : new IfStatementNode(node.getPosition(), condition, thenBody, elseBody);
    }

    @Override
    public ASTNode visit(WhileLoopNode node, Void unused) {
        Expression condition = transformExpression(node.getCondition());
        BodyNode body = transformBody(node.getBody());
        return condition == node.getCondition() && body == node.getBody() ? node
                : new WhileLoopNode(node.getPosition(), condition, body);
    }

    @Override
    public ASTNode visit(ForLoopNode node, Void unused) {
        RangeNode range = required(node.getRange(), RangeNode.class);
        BodyNode body = transformBody(node.getBody());
        return range == node.getRange() && body == node.getBody() ? node
                : new ForLoopNode(node.getPosition(), node.getVariableName(), range, node.isReverse(), body);
    }

    @Override
    public ASTNode visit(RangeNode node, Void unused) {
        Expression start = transformExpression(node.getStart());
        Expression end = transformExpression(node.getEnd());
        return start == node.getStart() && end == node.getEnd() ? node : new RangeNode(start, end);
    }

    @Override
    public ASTNode visit(PrintStatementNode node, Void unused) {
        List<Expression> expressions = rewriteList(node.getExpressions(), Expression.class);
        return expressions == node.getExpressions() ? node : new PrintStatementNode(node.getPosition(), expressions);
    }

    @Override
    public ASTNode visit(ReturnStatementNode node, Void unused) {
        Expression value = transformExpression(node.getValue());
        return value == node.getValue() ? node : new ReturnStatementNode(node.getPosition(), value);
    }

    @Override
    public ASTNode visit(RoutineCallStatementNode node, Void unused) {
        RoutineCallExpressionNode call = required(node.getCall(), RoutineCallExpressionNode.class);
        return call == node.getCall() ? node : new RoutineCallStatementNode(call);
    }

    @Override
    public ASTNode visit(BinaryExpressionNode node, Void unused) {
        Expression left = transformExpression(node.getLeft());
        Expression right = transformExpression(node.getRight());
        return left == node.getLeft() && right == node.getRight() ? node
                : new BinaryExpressionNode(left, node.getOperator(), right);
    }

    @Override
    public ASTNode visit(UnaryExpressionNode node, Void unused) {
        Expression operand = transformExpression(node.getOperand());
        return operand == node.getOperand() ? node : new UnaryExpressionNode(node.getPosition(), operand, node.getOperator());
    }

    @Override
    public ASTNode visit(CastExpressionNode node, Void unused) {
        Expression expression = transformExpression(node.getExpression());
        TypeNode type = transformType(node.getTargetType());
        return expression == node.getExpression() && type == node.getTargetType() ? node
                : new CastExpressionNode(node.getPosition(), expression, type);
    }

    @Override
    public ASTNode visit(ArrayAccessNode node, Void unused) {
        Expression array = transformExpression(node.getArray());
        Expression index = transformExpression(node.getIndex());
        return array == node.getArray() && index == node.getIndex() ? node : new ArrayAccessNode(node.getPosition(), array, index);
    }

    @Override
    public ASTNode visit(RecordAccessNode node, Void unused) {
        Expression record = transformExpression(node.getRecord());
        return record == node.getRecord() ? node : new RecordAccessNode(node.getPosition(), record, node.getFieldName());
    }

    @Override
    public ASTNode visit(RoutineCallExpressionNode node, Void unused) {
        List<Expression> arguments = rewriteList(node.getArguments(), Expression.class);
        return arguments == node.getArguments() ? node
                : new RoutineCallExpressionNode(node.getPosition(), node.getRoutineName(), arguments);
    }

    @Override public ASTNode visit(VariableReferenceNode node, Void unused) { return node; }
    @Override public ASTNode visit(IntLiteralNode node, Void unused) { return node; }
    @Override public ASTNode visit(RealLiteralNode node, Void unused) { return node; }
    @Override public ASTNode visit(BoolLiteralNode node, Void unused) { return node; }
    @Override public ASTNode visit(StringLiteralNode node, Void unused) { return node; }
    @Override public ASTNode visit(PrimitiveTypeNode node, Void unused) { return node; }
    @Override public ASTNode visit(NamedTypeNode node, Void unused) { return node; }

    @Override
    public ASTNode visit(ArrayTypeNode node, Void unused) {
        Expression size = transformExpression(node.getSize());
        TypeNode element = transformType(node.getElementType());
        return size == node.getSize() && element == node.getElementType() ? node
                : new ArrayTypeNode(node.getPosition(), size, element);
    }

    @Override
    public ASTNode visit(RecordTypeNode node, Void unused) {
        List<VariableDeclarationNode> fields = rewriteList(node.getFields(), VariableDeclarationNode.class);
        return fields == node.getFields() ? node : new RecordTypeNode(node.getPosition(), fields);
    }
}
