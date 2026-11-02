package ru.innopolis.semantic.type;

public abstract class Type {
    public abstract String getName();

    public boolean isError() {
        return false;
    }

    @Override
    public String toString() {
        return getName();
    }
}
