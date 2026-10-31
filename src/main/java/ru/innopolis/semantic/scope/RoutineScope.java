package ru.innopolis.semantic.scope;

import ru.innopolis.semantic.symbol.RoutineSymbol;

public class RoutineScope extends BaseScope {
    private final RoutineSymbol routine;

    public RoutineScope(IScope enclosingScope, RoutineSymbol routine) {
        super(enclosingScope);
        this.routine = routine;
    }

    public RoutineSymbol getRoutine() {
        return routine;
    }
}

