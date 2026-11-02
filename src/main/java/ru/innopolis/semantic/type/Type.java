package ru.innopolis.semantic.type;

/** Base class of semantic (resolved) types, as opposed to syntactic TypeNodes. */
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
