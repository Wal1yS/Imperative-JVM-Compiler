package ru.innopolis.semantic.symbol;

import ru.innopolis.ast.TypeNode;
import ru.innopolis.lexer.Span;

public class VariableSymbol extends Symbol {
    private final boolean isReadOnly;
    private final TypeNode syntacticType;
    private int readCount = 0;

    public VariableSymbol(String name, Span position, TypeNode syntacticType, boolean isReadOnly) {
        super(name, position);
        this.syntacticType = syntacticType;
        this.isReadOnly = isReadOnly;
    }

    public boolean isReadOnly() {
        return isReadOnly;
    }

    public TypeNode getSyntacticType() {
        return syntacticType;
    }

    public int getReadCount() {
        return readCount;
    }

    public void incrementReadCount() {
        this.readCount++;
    }
}

