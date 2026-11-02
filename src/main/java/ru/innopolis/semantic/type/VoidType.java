package ru.innopolis.semantic.type;

/** "Type" of a routine without a declared return type. */
public final class VoidType extends Type {
    public static final VoidType INSTANCE = new VoidType();

    private VoidType() {
    }

    @Override
    public String getName() {
        return "void";
    }
}
