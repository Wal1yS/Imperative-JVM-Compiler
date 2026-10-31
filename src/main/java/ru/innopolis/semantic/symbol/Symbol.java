package ru.innopolis.semantic.symbol;

import ru.innopolis.lexer.Span;

public abstract class Symbol {
    private final String name;
    private final Span position;

    public Symbol(String name, Span position) {
        this.name = name;
        this.position = position;
    }

    public String getName() {
        return name;
    }

    public Span getPosition() {
        return position;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" + name + " at " + position + "}";
    }
}

