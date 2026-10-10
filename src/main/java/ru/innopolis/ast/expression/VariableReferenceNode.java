package ru.innopolis.ast.expression;

import ru.innopolis.lexer.Span;

public class VariableReferenceNode extends Expression{
    private final String name;

    public VariableReferenceNode(Span position, String name) {
        super(position);
        this.name = name;
    }
}
