package ru.innopolis.ast.expression;

import ru.innopolis.ast.ASTNode;
import ru.innopolis.lexer.Span;

public abstract class Expression extends ASTNode {
    public Expression(Span position) {
        super(position);
    }
}

