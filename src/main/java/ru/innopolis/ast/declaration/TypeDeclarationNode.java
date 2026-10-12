package ru.innopolis.ast.declaration;

import ru.innopolis.ast.Declaration;
import ru.innopolis.lexer.Span;
import ru.innopolis.ast.TypeNode;

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
}

