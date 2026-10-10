package ru.innopolis.lexer;

public record Span(long lineNum, int posBegin, int posEnd) {
    public static Span combine(Span start, Span end) {
        return new Span(start.lineNum(), start.posBegin(), end.posEnd());
    }
}
