package ru.innopolis.ast;

import ru.innopolis.lexer.Span;
import ru.innopolis.semantic.visitor.ASTVisitor;

public abstract class ASTNode {
    private final Span position;

    public ASTNode(Span position) {
        this.position = position;
    }

    public Span getPosition(){
        return position;
    }

    public abstract <R, C> R accept(ASTVisitor<R, C> visitor, C context);
}
