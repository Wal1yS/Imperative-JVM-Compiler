package ru.innopolis.semantic.type;

import ru.innopolis.ast.TypeNode;
import ru.innopolis.ast.declaration.VariableDeclarationNode;
import ru.innopolis.ast.expression.Expression;
import ru.innopolis.ast.type.ArrayTypeNode;
import ru.innopolis.ast.type.NamedTypeNode;
import ru.innopolis.ast.type.PrimitiveTypeNode;
import ru.innopolis.ast.type.RecordTypeNode;
import ru.innopolis.semantic.context.SemanticContext;
import ru.innopolis.semantic.scope.IScope;
import ru.innopolis.semantic.symbol.Symbol;
import ru.innopolis.semantic.symbol.TypeSymbol;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;

/**
 * Converts syntactic TypeNodes into semantic Types.
 * Results are cached, so every declared type name and every record declaration maps to
 * exactly one Type object (record equivalence is by identity).
 */
public class TypeResolver {
    private final SemanticContext context;
    /** Types an expression (array sizes, untyped record member initializers). */
    private final BiFunction<Expression, IScope, Type> expressionTyper;

    private final Map<TypeSymbol, Type> namedTypes = new HashMap<>();
    private final Map<TypeNode, Type> nodeTypes = new HashMap<>();
    private final Set<TypeSymbol> inProgress = new HashSet<>();

    public TypeResolver(SemanticContext context, BiFunction<Expression, IScope, Type> expressionTyper) {
        this.context = context;
        this.expressionTyper = expressionTyper;
    }

    /** Resolves a type node; a null node (no declared return type) means void. */
    public Type resolve(TypeNode node, IScope scope) {
        if (node == null) return VoidType.INSTANCE;
        if (node instanceof PrimitiveTypeNode p) return resolvePrimitive(p);
        if (node instanceof NamedTypeNode n) return resolveNamed(n, scope);

        Type cached = nodeTypes.get(node);
        if (cached != null) return cached;
        Type result;
        if (node instanceof ArrayTypeNode a) {
            result = resolveArray(a, scope);
        } else if (node instanceof RecordTypeNode r) {
            result = resolveRecord(r, "record", scope);
        } else {
            result = ErrorType.INSTANCE;
        }
        nodeTypes.put(node, result);
        return result;
    }

    /** Resolves a declared type name, e.g. for a TypeDeclarationNode. */
    public Type resolveSymbol(TypeSymbol symbol, IScope scope) {
        Type cached = namedTypes.get(symbol);
        if (cached != null) return cached;
        if (inProgress.contains(symbol)) {
            context.addError(symbol.getPosition(), "Recursive type definition '" + symbol.getName() + "' is not supported");
            return ErrorType.INSTANCE;
        }

        inProgress.add(symbol);
        IScope declaringScope = declaringScope(symbol, scope);
        Type result;
        if (symbol.getDeclaredType() instanceof RecordTypeNode r) {
            // A named record takes the name of its type declaration (used in error messages)
            result = nodeTypes.get(r);
            if (result == null) {
                result = resolveRecord(r, symbol.getName(), declaringScope);
                nodeTypes.put(r, result);
            }
        } else {
            result = resolve(symbol.getDeclaredType(), declaringScope);
        }
        inProgress.remove(symbol);

        namedTypes.put(symbol, result);
        return result;
    }

    private Type resolvePrimitive(PrimitiveTypeNode node) {
        return switch (node.getKind()) {
            case INTEGER -> PrimitiveType.INTEGER;
            case REAL -> PrimitiveType.REAL;
            case BOOLEAN -> PrimitiveType.BOOLEAN;
            case STRING -> PrimitiveType.STRING;
            default -> ErrorType.INSTANCE;
        };
    }

    private Type resolveNamed(NamedTypeNode node, IScope scope) {
        Symbol sym = scope.resolve(node.getName());
        if (!(sym instanceof TypeSymbol typeSym)) {
            // Already reported by the declaration pass
            return ErrorType.INSTANCE;
        }
        return resolveSymbol(typeSym, scope);
    }

    private Type resolveArray(ArrayTypeNode node, IScope scope) {
        Type element = resolve(node.getElementType(), scope);
        if (node.getSize() == null) {
            return new ArrayType(element, null, false);
        }

        Type sizeType = expressionTyper.apply(node.getSize(), scope);
        if (!sizeType.isError() && sizeType != PrimitiveType.INTEGER) {
            context.addError(node.getSize().getPosition(), "Array size must be integer, got " + sizeType);
            return new ArrayType(element, null, true);
        }
        // A non-constant size is allowed (run-time sized local arrays); it is just unknown here
        Integer size = ConstantEvaluator.evaluateInt(node.getSize());
        if (size != null && size <= 0) {
            context.addError(node.getSize().getPosition(), "Array size must be positive, got " + size);
            size = null;
        }
        return new ArrayType(element, size, true);
    }

    private Type resolveRecord(RecordTypeNode node, String name, IScope scope) {
        RecordType record = new RecordType(name);
        // Members are resolved in the record's own scope created by the declaration pass
        IScope fieldScope = context.getNodeScope(node) != null ? context.getNodeScope(node) : scope;
        for (VariableDeclarationNode field : node.getFields()) {
            Type fieldType;
            if (field.getType() != null) {
                fieldType = resolve(field.getType(), fieldScope);
            } else if (field.getInitializer() != null) {
                fieldType = expressionTyper.apply(field.getInitializer(), fieldScope);
            } else {
                fieldType = ErrorType.INSTANCE;
            }
            record.addField(field.getName(), fieldType);
        }
        return record;
    }

    private static IScope declaringScope(TypeSymbol symbol, IScope scope) {
        IScope current = scope;
        while (current != null) {
            if (current.findInScope(symbol.getName()) == symbol) return current;
            current = current.getEnclosingScope();
        }
        return scope;
    }
}
