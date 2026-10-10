package ru.innopolis.ast.expression;

import ru.innopolis.lexer.Span;

public class IntLiteralNode extends Expression{
    private final int value;

    public IntLiteralNode(Span position, int value) {
        super(position);
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
