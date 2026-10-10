package ru.innopolis.ast.expression;

import ru.innopolis.lexer.Span;

public class ArrayAccessNode extends Expression {
    private final Expression array;
    private final Expression index;

    public ArrayAccessNode(Span position, Expression array, Expression index) {
        super(position);
        this.array = array;
        this.index = index;
    }
}
