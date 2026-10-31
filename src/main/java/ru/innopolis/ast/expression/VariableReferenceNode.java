package ru.innopolis.ast.expression;

import ru.innopolis.lexer.Span;
import ru.innopolis.semantic.visitor.ASTVisitor;

public class VariableReferenceNode extends Expression{
    private final String name;

    public VariableReferenceNode(Span position, String name) {
        super(position);
        this.name = name;
    }

    public String getName() {
        return name;
    }

    @Override
    public <R, C> R accept(ASTVisitor<R, C> visitor, C context) {
        return visitor.visit(this, context);
    }
}
