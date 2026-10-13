package ru.innopolis.ast.type;

import ru.innopolis.ast.TypeNode;
import ru.innopolis.lexer.Span;
import ru.innopolis.lexer.TokenType;

public class PrimitiveTypeNode extends TypeNode {
    private final TokenType kind;

    public PrimitiveTypeNode(Span position, TokenType kind) {
        super(position);
        this.kind = kind;
    }

    public TokenType getKind() {
        return kind;
    }
}

