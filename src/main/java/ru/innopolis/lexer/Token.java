package ru.innopolis.lexer;

abstract class Token {
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
}

class IntToken extends Token{
    private int value;

    public IntToken(Span position, int value){
        super(TokenType.INT_LITERAL, position);
        this.value = value;
    }

    public int getValue(){
        return this.value;
    }
}

class RealToken extends Token{
    private double value;

    public RealToken(Span position, double value){
        super(TokenType.REAL_LITERAL, position);
        this.value = value;
    }

    public double getValue(){
        return this.value;
    }
}

class StringToken extends Token{
    private String value;

    public StringToken(Span position, String value){
        super(TokenType.STRING_LITERAL, position);
        this.value = value;
    }

    public String getValue(){
        return this.value;
    }
}

class IdentifierToken extends Token{
    private String name;

    public IdentifierToken(Span position, String name){
        super(TokenType.IDENTIFIER, position);
        this.name = name;
    }

    public String getName(){
        return this.name;
    }
}

final class OperatorToken extends Token {
    public OperatorToken(TokenType type, Span span) {
        super(type, span);
    }
}


