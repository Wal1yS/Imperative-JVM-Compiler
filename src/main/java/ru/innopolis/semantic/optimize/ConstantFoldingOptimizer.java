package ru.innopolis.semantic.optimize;

import ru.innopolis.ast.ASTNode;
import ru.innopolis.ast.ProgramNode;
import ru.innopolis.ast.declaration.VariableDeclarationNode;
import ru.innopolis.ast.expression.*;
import ru.innopolis.ast.statement.BodyNode;
import ru.innopolis.semantic.symbol.ParameterSymbol;
import ru.innopolis.semantic.symbol.Symbol;
import ru.innopolis.semantic.symbol.VariableSymbol;
import ru.innopolis.semantic.context.SemanticContext;
import ru.innopolis.semantic.type.ConstantEvaluator;
import ru.innopolis.semantic.type.PrimitiveType;
import ru.innopolis.semantic.type.Type;
import ru.innopolis.semantic.visitor.ASTTransformer;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

/** Folds checked constant expressions using the same evaluator as the static checks. */
public class ConstantFoldingOptimizer extends ASTTransformer {
    private final Set<Symbol> initializedLocals = Collections.newSetFromMap(new IdentityHashMap<>());

    public ConstantFoldingOptimizer(SemanticContext context) {
        super(context);
    }

    @Override
    public ASTNode visit(ProgramNode node, Void unused) {
        initializedLocals.clear();
        return super.visit(node, unused);
    }

    @Override
    public ASTNode visit(BodyNode node, Void unused) {
        // Local references are already bound by Pass 1. Reaching a later reference means its
        // declaration's initializer completed; this does not treat the value as a constant.
        for (ASTNode item : node.getItems()) {
            if (item instanceof VariableDeclarationNode variable && variable.getInitializer() != null) {
                Symbol symbol = context.getDeclaredSymbol(variable);
                if (symbol != null) initializedLocals.add(symbol);
            }
        }
        return super.visit(node, unused);
    }

    @Override
    public ASTNode visit(BinaryExpressionNode node, Void unused) {
        return fold(node, (Expression) super.visit(node, unused));
    }

    @Override
    public ASTNode visit(UnaryExpressionNode node, Void unused) {
        return fold(node, (Expression) super.visit(node, unused));
    }

    @Override
    public ASTNode visit(CastExpressionNode node, Void unused) {
        return fold(node, (Expression) super.visit(node, unused));
    }

    private Expression fold(Expression original, Expression rewritten) {
        // Do not use evaluation to bypass a type error or invent rules for an unchecked node.
        Type type = context.getExpressionType(original);
        if (type == null || type.isError()) return rewritten;
        Object value = ConstantEvaluator.evaluate(rewritten);
        if (value instanceof Integer integer && type == PrimitiveType.INTEGER) {
            return new IntLiteralNode(original.getPosition(), integer);
        }
        if (value instanceof Double real && type == PrimitiveType.REAL) {
            return new RealLiteralNode(original.getPosition(), real);
        }
        if (value instanceof Boolean bool && type == PrimitiveType.BOOLEAN) {
            return new BoolLiteralNode(original.getPosition(), bool);
        }
        if (value instanceof String string && type == PrimitiveType.STRING) {
            return new StringLiteralNode(original.getPosition(), string);
        }
        if (type == PrimitiveType.INTEGER && rewritten instanceof BinaryExpressionNode binary) {
            Expression left = binary.getLeft();
            Expression right = binary.getRight();
            switch (binary.getOperator()) {
                case PLUS -> {
                    if (integerIs(right, 0)) return left;
                    if (integerIs(left, 0)) return right;
                }
                case MULT -> {
                    if (integerIs(right, 1)) return left;
                    if (integerIs(left, 1)) return right;
                    if ((integerIs(right, 0) && safeToDiscard(left))
                            || (integerIs(left, 0) && safeToDiscard(right))) {
                        return new IntLiteralNode(original.getPosition(), 0);
                    }
                }
                default -> { }
            }
        }
        // Do not erase unknown computations, calls or operations that may fail (e.g. 1 / 0).
        return rewritten;
    }

    private boolean integerIs(Expression expression, int value) {
        return expression instanceof IntLiteralNode literal && literal.getValue() == value;
    }

    private boolean safeToDiscard(Expression expression) {
        if (expression instanceof IntLiteralNode) return true;
        if (!(expression instanceof VariableReferenceNode reference)) return false;
        Symbol symbol = context.getResolvedSymbol(reference);
        return symbol instanceof ParameterSymbol || initializedLocals.contains(symbol)
                || symbol instanceof VariableSymbol variable && variable.isReadOnly();
    }
}
