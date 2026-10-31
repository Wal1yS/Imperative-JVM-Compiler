package ru.innopolis.semantic.scope;

import ru.innopolis.semantic.symbol.Symbol;

import java.util.LinkedHashMap;
import java.util.Map;

public abstract class BaseScope implements IScope {
    protected final IScope enclosingScope;
    protected final Map<String, Symbol> symbols = new LinkedHashMap<>();

    public BaseScope(IScope enclosingScope) {
        this.enclosingScope = enclosingScope;
    }

    @Override
    public IScope getEnclosingScope() {
        return enclosingScope;
    }

    @Override
    public void define(Symbol symbol) {
        symbols.put(symbol.getName(), symbol);
    }

    @Override
    public Symbol findInScope(String name) {
        return symbols.get(name);
    }

    @Override
    public Symbol resolve(String name) {
        Symbol s = findInScope(name);
        if (s != null) {
            return s;
        }
        if (enclosingScope != null) {
            return enclosingScope.resolve(name);
        }
        return null;
    }

    public Map<String, Symbol> getSymbols() {
        return symbols;
    }
}

