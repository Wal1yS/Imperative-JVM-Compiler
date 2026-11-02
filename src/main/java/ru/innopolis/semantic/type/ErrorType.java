package ru.innopolis.semantic.type;

/**
 * Type of an erroneous expression. It is compatible with everything, so an error is reported
 * once and does not cause induced errors in the enclosing expressions.
 */
public final class ErrorType extends Type {
    public static final ErrorType INSTANCE = new ErrorType();

    private ErrorType() {
    }

    @Override
    public String getName() {
        return "<error>";
    }

    @Override
    public boolean isError() {
        return true;
    }
}
