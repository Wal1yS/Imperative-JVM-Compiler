package ru.innopolis.ast;

import ru.innopolis.lexer.Span;

public abstract class ASTNode {
    private final Span position;

    public ASTNode(Span position) {
        this.position = position;
    }

    public Span getPosition(){
        return position;
    }
}
