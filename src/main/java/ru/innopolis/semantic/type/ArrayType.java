package ru.innopolis.semantic.type;

import ru.innopolis.ast.expression.Expression;

public final class ArrayType extends Type {
    private final Type elementType;
    private final Integer size;
    private final boolean sized;
    private final Expression sizeExpression;

    public ArrayType(Type elementType, Integer size, boolean sized) {
        this(elementType, size, sized, null);
    }

    ArrayType(Type elementType, Integer size, boolean sized, Expression sizeExpression) {
        this.elementType = elementType;
        this.size = size;
        this.sized = sized;
        this.sizeExpression = sizeExpression;
    }

    public Expression getSizeExpression() {
        return sizeExpression;
    }

    public Type getElementType() {
        return elementType;
    }

    public Integer getSize() {
        return size;
    }

    public boolean isSized() {
        return sized;
    }

    @Override
    public String getName() {
        return "array " + (size != null ? "[" + size + "] " : "") + elementType.getName();
    }
}
