package ru.innopolis.ast.expression;

import ru.innopolis.lexer.Span;

public class BoolLiteralNode extends Expression{
    private final boolean value;

    public BoolLiteralNode(Span position, boolean value) {
        super(position);
        this.value = value;
    }

    public boolean getValue() {
        return value;
    }
}
