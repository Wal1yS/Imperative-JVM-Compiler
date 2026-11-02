package ru.innopolis.semantic.type;

public final class ArrayType extends Type {
    private final Type elementType;
    private final Integer size;
    private final boolean sized;

    public ArrayType(Type elementType, Integer size, boolean sized) {
        this.elementType = elementType;
        this.size = size;
        this.sized = sized;
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
