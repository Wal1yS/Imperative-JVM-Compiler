package ru.innopolis.ast.expression;

import ru.innopolis.lexer.Span;
import ru.innopolis.semantic.visitor.ASTVisitor;

public class IntLiteralNode extends Expression{
    private final int value;

    public IntLiteralNode(Span position, int value) {
        super(position);
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    @Override
    public <R, C> R accept(ASTVisitor<R, C> visitor, C context) {
        return visitor.visit(this, context);
    }
}
