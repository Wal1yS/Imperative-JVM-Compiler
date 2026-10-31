package ru.innopolis.ast.expression;

import ru.innopolis.ast.TypeNode;
import ru.innopolis.lexer.Span;
import ru.innopolis.semantic.visitor.ASTVisitor;

public class CastExpressionNode extends Expression {
    private final Expression expression;
    private final TypeNode targetType;

    public CastExpressionNode(Span position, Expression expression, TypeNode targetType) {
        super(position);
        this.expression = expression;
        this.targetType = targetType;
    }

    public Expression getExpression() {
        return expression;
    }

    public TypeNode getTargetType() {
        return targetType;
    }

    @Override
    public <R, C> R accept(ASTVisitor<R, C> visitor, C context) {
        return visitor.visit(this, context);
    }
}
