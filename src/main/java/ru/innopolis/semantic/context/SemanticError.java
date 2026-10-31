package ru.innopolis.semantic.context;

import ru.innopolis.lexer.Span;

public class SemanticError {
    private final Span position;
    private final String message;

    public SemanticError(Span position, String message) {
        this.position = position;
        this.message = message;
    }

    public Span getPosition() {
        return position;
    }

    public String getMessage() {
        return message;
    }

    @Override
    public String toString() {
        if (position == null) {
            return "Semantic Error: " + message;
        }
        return "Semantic Error at " + position + ": " + message;
    }
}

