package ru.innopolis.ast.expression;

import ru.innopolis.lexer.Span;
import ru.innopolis.lexer.TokenType;
import ru.innopolis.semantic.visitor.ASTVisitor;

public class BinaryExpressionNode extends Expression {
    private final Expression left;
    private final TokenType operator;
    private final Expression right;

    public BinaryExpressionNode(Expression left, TokenType type, Expression right) {
        super(Span.combine(left.getPosition(), right.getPosition()));
        this.left = left;
        this.right = right;
        this.operator = type;
    }

    public Expression getLeft(){
        return left;
    }

    public Expression getRight() {
        return right;
    }

    public TokenType getOperator() {
        return operator;
    }

    @Override
    public <R, C> R accept(ASTVisitor<R, C> visitor, C context) {
        return visitor.visit(this, context);
    }
}
