package ru.innopolis.lexer.token;

import ru.innopolis.lexer.Span;
import ru.innopolis.lexer.TokenType;

public class RealToken extends Token{
    private double value;

    public RealToken(Span position, double value){
        super(TokenType.REAL_LITERAL, position);
        this.value = value;
    }

    public double getValue(){
        return this.value;
    }

    @Override
    public String toString() {
        return String.format("[%s %f at %s]", getType(), value, getPosition());
    }
}
