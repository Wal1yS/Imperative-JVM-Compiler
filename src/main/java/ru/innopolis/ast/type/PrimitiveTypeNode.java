package ru.innopolis.ast.type;

import ru.innopolis.ast.TypeNode;
import ru.innopolis.lexer.Span;
import ru.innopolis.lexer.TokenType;
import ru.innopolis.semantic.visitor.ASTVisitor;

public class PrimitiveTypeNode extends TypeNode {
    private final TokenType kind;

    public PrimitiveTypeNode(Span position, TokenType kind) {
        super(position);
        this.kind = kind;
    }

    public TokenType getKind() {
        return kind;
    }

    @Override
    public <R, C> R accept(ASTVisitor<R, C> visitor, C context) {
        return visitor.visit(this, context);
    }
}
