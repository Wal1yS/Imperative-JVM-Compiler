package ru.innopolis.ast.statement;

import ru.innopolis.ast.ASTNode;
import ru.innopolis.lexer.Span;
import ru.innopolis.semantic.visitor.ASTVisitor;

import java.util.List;

public class BodyNode extends ASTNode {
    private final List<ASTNode> items;

    public BodyNode(Span position, List<ASTNode> items) {
        super(position);
        this.items = List.copyOf(items);
    }

    public List<ASTNode> getItems() {
        return items;
    }

    @Override
    public <R, C> R accept(ASTVisitor<R, C> visitor, C context) {
        return visitor.visit(this, context);
    }
}
