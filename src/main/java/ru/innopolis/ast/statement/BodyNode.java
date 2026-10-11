package ru.innopolis.ast.statement;

import ru.innopolis.ast.ASTNode;
import ru.innopolis.lexer.Span;

import java.util.List;

public class BodyNode extends ASTNode {
    private final List<ASTNode> items;

    public BodyNode(Span position, List<ASTNode> items) {
        super(position);
        this.items = items;
    }

    public List<ASTNode> getItems() {
        return items;
    }
}
