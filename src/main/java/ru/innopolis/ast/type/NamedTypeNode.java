package ru.innopolis.ast.type;

import ru.innopolis.ast.TypeNode;
import ru.innopolis.lexer.Span;


public class NamedTypeNode extends TypeNode {
    private final String name;

    public NamedTypeNode(Span position, String name) {
        super(position);
        this.name = name;
    }

    public String getName() {
        return name;
    }
}

