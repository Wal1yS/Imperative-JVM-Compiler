package ru.innopolis.lexer.token;

import ru.innopolis.lexer.Span;
import ru.innopolis.lexer.TokenType;

public abstract class Token {
    private final TokenType type;
    private final Span position;

    public Token(TokenType type, Span position){
        this.type = type;
        this.position = position;
    }

    public TokenType getType(){
        return this.type;
    }

    public Span getPosition(){
        return this.position;
    }

    @Override
    public String toString() {
        return String.format("[%s at %s]", type, position);
    }
}


