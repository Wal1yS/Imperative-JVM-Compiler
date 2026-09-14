package ru.innopolis.lexer;

import java.util.ArrayList;
import java.util.List;

public class Scanner {
    private final String code;
    private final List<Token> tokens = new ArrayList<>();
    private static final java.util.Map<String, TokenType> keywords = new java.util.HashMap<>();
    static {
        keywords.put("routine", TokenType.ROUTINE);
        keywords.put("var", TokenType.VAR);
        keywords.put("is", TokenType.IS);
        keywords.put("if", TokenType.IF);
        keywords.put("then", TokenType.THEN);
        keywords.put("else", TokenType.ELSE);
        keywords.put("end", TokenType.END);
        keywords.put("while", TokenType.WHILE);
        keywords.put("loop", TokenType.LOOP);
        keywords.put("for", TokenType.FOR);
        keywords.put("in", TokenType.IN);
        keywords.put("reverse", TokenType.REVERSE);
        keywords.put("return", TokenType.RETURN);
        keywords.put("print", TokenType.PRINT);

        keywords.put("type", TokenType.TYPE);
        keywords.put("record", TokenType.RECORD);
        keywords.put("array", TokenType.ARRAY);
        keywords.put("integer", TokenType.INTEGER);
        keywords.put("real", TokenType.REAL);
        keywords.put("boolean", TokenType.BOOLEAN);
        keywords.put("string", TokenType.STRING);

        keywords.put("true", TokenType.TRUE);
        keywords.put("false", TokenType.FALSE);
        keywords.put("and", TokenType.AND);
        keywords.put("or", TokenType.OR);
        keywords.put("xor", TokenType.XOR);
        keywords.put("not", TokenType.NOT);
        keywords.put("as", TokenType.AS);
    }
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
        char c = step();
        switch (c) {
            case ' ': case '\r': case '\t': break;
            case '\n': line++; column = 1; break;

            case '+': addToken(TokenType.PLUS); break;
            case '-': addToken(TokenType.MINUS); break;
            case '*': addToken(TokenType.MULT); break;
            case '%': addToken(TokenType.MOD); break;
            case '(': addToken(TokenType.LPAREN); break;
            case ')': addToken(TokenType.RPAREN); break;
            case '[': addToken(TokenType.LBRACKET); break;
            case ']': addToken(TokenType.RBRACKET); break;
            case ',': addToken(TokenType.COMMA); break;
            case '=': addToken(TokenType.EQUAL); break;
            case ';': addToken(TokenType.SEMICOLON); break;

            case ':': addToken(match('=') ? TokenType.ASSIGN : TokenType.COLON); break;
            case '.': addToken(match('.') ? TokenType.DOT_DOT : TokenType.DOT); break;
            case '<': addToken(match('=') ? TokenType.LESS_EQUAL : TokenType.LESS); break;
            case '>': addToken(match('=') ? TokenType.GREATER_EQUAL : TokenType.GREATER); break;

            case '/':
                if (match('=')) {
                    addToken(TokenType.NOT_EQUAL);
                } else if (match('/')) {
                    while (peek() != '\n' && !isAtEnd()) step();
                } else if (match('*')) {
                    while (!isAtEnd()) {
                        if (peek() == '\n') {
                            line++;
                            column = 1;
                        }

                        if (peek() == '*' && peekNext() == '/') {
                            step();
                            step();
                            break;
                        }

                        step();
                    }
                } else {
                    addToken(TokenType.DIV);
                }
                break;

            case '"':
                while (peek() != '"' && !isAtEnd()) {
                    if (peek() == '\n') {
                        line++;
                        column = 1;
                    }
                    step();
                }

                if (isAtEnd()) {
                    System.err.println("Lexical error: Unterminated string at line " + line);
                    return;
                }

                step();

                String text = code.substring(start + 1, current - 1);
                Span stringSpan = new Span(line, startColumn, column - 1);
                tokens.add(new StringToken(stringSpan, text));
                break;

            default:
                if (isDigit(c)) {
                    while (isDigit(peek())) step();

                    if (peek() == '.' && isDigit(peekNext())) {
                        step();
                        while (isDigit(peek())) step();

                        double value = Double.parseDouble(code.substring(start, current));
                        Span numberSpan = new Span(line, startColumn, column - 1);
                        tokens.add(new RealToken(numberSpan, value));
                    } else {
                        int value = Integer.parseInt(code.substring(start, current));
                        Span numberSpan = new Span(line, startColumn, column - 1);
                        tokens.add(new IntToken(numberSpan, value));
                    }
                } else if (isAlpha(c)) {
                    while (isAlphaNumeric(peek())) step();

                    String name = code.substring(start, current);

                    TokenType type = keywords.get(name);
                    Span span = new Span(line, startColumn, column - 1);

                    if (type == null) {
                        tokens.add(new IdentifierToken(span, name));
                    } else {
                        tokens.add(new OperatorToken(type, span));
                    }
                } else {
                    System.err.println("Lexical error: Unexpected character '" + c +
                            "' at line " + line + ":" + startColumn);
                }
                break;
        }
    }
    private boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }

    private boolean isAlpha(char c) {
        return (c >= 'a' && c <= 'z') ||
                (c >= 'A' && c <= 'Z') ||
                c == '_';
    }

    private boolean isAlphaNumeric(char c) {
        return isAlpha(c) || isDigit(c);
    }
}
