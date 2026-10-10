package ru.innopolis.lexer.token;

import ru.innopolis.lexer.Span;
import ru.innopolis.lexer.TokenType;

public class IntToken extends Token{
    private int value;

    public IntToken(Span position, int value){
        super(TokenType.INT_LITERAL, position);
        this.value = value;
    }

    public int getValue(){
        return this.value;
    }

    @Override
    public String toString() {
        return String.format("[%s %d at %s]", getType(), value, getPosition());
    }
}
