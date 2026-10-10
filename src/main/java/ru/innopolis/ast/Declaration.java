package ru.innopolis.ast;

import ru.innopolis.lexer.Span;

public abstract class Declaration extends ASTNode{
    public Declaration(Span position) {
        super(position);
    }
}
