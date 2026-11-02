package ru.innopolis.semantic.type;

/** Built-in types. Each is a singleton, so primitive types are compared by identity. */
public final class PrimitiveType extends Type {
    public static final PrimitiveType INTEGER = new PrimitiveType("integer", true);
    public static final PrimitiveType REAL = new PrimitiveType("real", true);
    public static final PrimitiveType BOOLEAN = new PrimitiveType("boolean", false);
    public static final PrimitiveType STRING = new PrimitiveType("string", false);

    private final String name;
    private final boolean numeric;

    private PrimitiveType(String name, boolean numeric) {
        this.name = name;
        this.numeric = numeric;
    }

    public boolean isNumeric() {
        return numeric;
    }

    @Override
    public String getName() {
        return name;
    }
}
