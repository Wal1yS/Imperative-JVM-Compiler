package ru.innopolis.ast;

import ru.innopolis.lexer.Span;

public abstract class TypeNode extends ASTNode{
    public TypeNode(Span position) {
        super(position);
    }
}
