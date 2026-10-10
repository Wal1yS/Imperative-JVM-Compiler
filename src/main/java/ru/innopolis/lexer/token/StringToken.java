package ru.innopolis.lexer.token;

import ru.innopolis.lexer.Span;
import ru.innopolis.lexer.TokenType;

public class StringToken extends Token{
    private String value;

    public StringToken(Span position, String value){
        super(TokenType.STRING_LITERAL, position);
        this.value = value;
    }

    public String getValue(){
        return this.value;
    }

    @Override
    public String toString() {
        return String.format("[%s \"%s\" at %s]", getType(), value, getPosition());
    }
}
