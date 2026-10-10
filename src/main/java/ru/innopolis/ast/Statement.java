package ru.innopolis.ast;

import ru.innopolis.lexer.Span;

public abstract class Statement extends ASTNode{
    public Statement(Span position) {
        super(position);
    }
}
