package ru.innopolis.semantic.scope;

import ru.innopolis.semantic.symbol.Symbol;

public interface IScope {
    IScope getEnclosingScope();

    void define(Symbol symbol);

    /** Looks up symbol strictly in the current scope level (for duplicate detection). */
    Symbol findInScope(String name);

    /** Looks up symbol recursively up the scope chain. */
    Symbol resolve(String name);
}

