package ru.innopolis.semantic.symbol;

import ru.innopolis.ast.TypeNode;
import ru.innopolis.lexer.Span;

public class TypeSymbol extends Symbol {
    private final TypeNode declaredType;

    public TypeSymbol(String name, Span position, TypeNode declaredType) {
        super(name, position);
        this.declaredType = declaredType;
    }

    public TypeNode getDeclaredType() {
        return declaredType;
    }
}

