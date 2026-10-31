package ru.innopolis.ast.expression;

import ru.innopolis.lexer.Span;
import ru.innopolis.semantic.visitor.ASTVisitor;

public class RecordAccessNode extends Expression {
    private final Expression record;
    private final String fieldName;

    public RecordAccessNode(Span position, Expression record, String fieldName) {
        super(position);
        this.record = record;
        this.fieldName = fieldName;
    }

    public Expression getRecord() {
        return record;
    }

    public String getFieldName() {
        return fieldName;
    }

    @Override
    public <R, C> R accept(ASTVisitor<R, C> visitor, C context) {
        return visitor.visit(this, context);
    }
}
