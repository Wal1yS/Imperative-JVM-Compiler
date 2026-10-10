package ru.innopolis.lexer.token;

import ru.innopolis.lexer.Span;
import ru.innopolis.lexer.TokenType;

public final class OperatorToken extends Token {
    public OperatorToken(TokenType type, Span span) {
        super(type, span);
    }
}
