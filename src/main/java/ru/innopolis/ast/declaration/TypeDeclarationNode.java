package ru.innopolis.ast.declaration;

import ru.innopolis.ast.Declaration;
import ru.innopolis.lexer.Span;
import ru.innopolis.ast.TypeNode;
import ru.innopolis.semantic.visitor.ASTVisitor;

public class TypeDeclarationNode extends Declaration {
    private final String name;
    private final TypeNode type;

    public TypeDeclarationNode(Span position, String name, TypeNode type) {
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
