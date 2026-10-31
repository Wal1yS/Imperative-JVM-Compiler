package ru.innopolis.semantic.symbol;

import ru.innopolis.ast.TypeNode;
import ru.innopolis.lexer.Span;

public class ParameterSymbol extends Symbol {
    private final TypeNode typeNode;

    public ParameterSymbol(String name, Span position, TypeNode typeNode) {
        super(name, position);
        this.typeNode = typeNode;
    }

    public TypeNode getTypeNode() {
        return typeNode;
    }
}

