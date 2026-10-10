package ru.innopolis.lexer.token;

import ru.innopolis.lexer.Span;
import ru.innopolis.lexer.TokenType;

public class IdentifierToken extends Token{
    private String name;

    public IdentifierToken(Span position, String name){
        super(TokenType.IDENTIFIER, position);
        this.name = name;
    }

    public String getName(){
        return this.name;
    }

    @Override
    public String toString() {
        return String.format("[%s '%s' at %s]", getType(), name, getPosition());
    }
}
