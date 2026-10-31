package ru.innopolis.ast.declaration;

import ru.innopolis.ast.ASTNode;
import ru.innopolis.lexer.Span;
import ru.innopolis.ast.TypeNode;
import ru.innopolis.semantic.visitor.ASTVisitor;

public class ParameterNode extends ASTNode {
    private final String name;
    private final TypeNode type;

    public ParameterNode(Span position, String name, TypeNode type) {
        super(position);
        this.name = name;
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public TypeNode getType() {
        return type;
    }

    @Override
    public <R, C> R accept(ASTVisitor<R, C> visitor, C context) {
        return visitor.visit(this, context);
    }
}
