package ru.innopolis.semantic.type;

/**
 * Array type. Arrays are compared structurally (element type and size), so that a named
 * array type can be passed to a sizeless 'array T' parameter.
 */
public final class ArrayType extends Type {
    private final Type elementType;
    /** Compile-time size, or null when the size is omitted or only known at run time. */
    private final Integer size;
    /** False for the sizeless form 'array T', allowed only for parameters and return types. */
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
