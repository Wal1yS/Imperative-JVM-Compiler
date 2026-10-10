package ru.innopolis.ast.expression;

import ru.innopolis.lexer.Span;

public class RecordAccessNode extends Expression {
    private final Expression record;
    private final String fieldName;

    public RecordAccessNode(Span position, Expression record, String fieldName) {
        super(position);
        this.record = record;
        this.fieldName = fieldName;
    }
}
