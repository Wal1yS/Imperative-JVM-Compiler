package ru.innopolis.semantic.type;

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
