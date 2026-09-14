package ru.innopolis.lexer;

import java.util.ArrayList;
import java.util.List;

public class Scanner {
    private final String code;
    private final List<Token> tokens = new ArrayList<>();
    private int start = 0;
    private int current = 0;
    private int line = 1;
    private int column = 1;
    private int startColumn = 1;

    public Scanner(String code){
        this.code = code;
    }

    private boolean isAtEnd(){
        return current >= code.length();
    }

    private char step(){
        char character = code.charAt(current);
        current += 1;
        column += 1;
        return character;
    }

    private char peek(){
        if (isAtEnd()) return '\0';
        return code.charAt(current);
    }

    private char peekNext() {
        if (current + 1 >= code.length()) return '\0';
        return code.charAt(current + 1);
    }

    private boolean match(char expected) {
        if (isAtEnd()) return false;
        if (code.charAt(current) != expected) return false;

        current += 1;
        column += 1;
        return true;
    }

    private void addToken(TokenType type) {
        Span span = new Span(line, startColumn, column - 1);
        tokens.add(new OperatorToken(type, span));
    }

    public List<Token> scanTokens(){
        while (!isAtEnd()) {
            start = current;
            startColumn = column;
            scanToken();
        }
        Span eofSpan = new Span(line, column, column);
        tokens.add(new OperatorToken(TokenType.EOF, eofSpan));
        return tokens;
    }

    private void scanToken() {
        char c = peek();
        switch (c) {
            case ' ':
            case '\r':
            case '\t':
                break;

            case '\n':
                line++;
                column = 1;
                break;

            case '+':
                addToken(TokenType.PLUS);
                break;

            default:
                System.err.println("Unexpected character '" + c + "' at line " + line + ", column " + startColumn);
                break;
        }
    }
}
