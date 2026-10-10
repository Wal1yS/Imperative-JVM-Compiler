package ru.innopolis.ast.expression;

import ru.innopolis.lexer.Span;

public class RealLiteralNode extends Expression{
    private final double value;

    public RealLiteralNode(Span position, double value) {
        super(position);
        this.value = value;
    }

    public double getValue() {
        return value;
    }
}
