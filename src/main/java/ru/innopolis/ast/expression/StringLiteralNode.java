package ru.innopolis.ast.expression;

import ru.innopolis.lexer.Span;

public class StringLiteralNode extends Expression{
    private final String value;

    public StringLiteralNode(Span position, String value) {
        super(position);
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
