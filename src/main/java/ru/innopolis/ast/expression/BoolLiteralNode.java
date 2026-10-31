package ru.innopolis.ast.expression;

import ru.innopolis.lexer.Span;
import ru.innopolis.semantic.visitor.ASTVisitor;

public class BoolLiteralNode extends Expression{
    private final boolean value;

    public BoolLiteralNode(Span position, boolean value) {
        super(position);
        this.value = value;
    }

    public boolean getValue() {
        return value;
    }

    @Override
    public <R, C> R accept(ASTVisitor<R, C> visitor, C context) {
        return visitor.visit(this, context);
    }
}
