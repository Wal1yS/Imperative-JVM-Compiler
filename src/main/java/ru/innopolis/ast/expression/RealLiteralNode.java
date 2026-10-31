package ru.innopolis.ast.expression;

import ru.innopolis.lexer.Span;
import ru.innopolis.semantic.visitor.ASTVisitor;

public class RealLiteralNode extends Expression{
    private final double value;

    public RealLiteralNode(Span position, double value) {
        super(position);
        this.value = value;
    }

    public double getValue() {
        return value;
    }

    @Override
    public <R, C> R accept(ASTVisitor<R, C> visitor, C context) {
        return visitor.visit(this, context);
    }
}
