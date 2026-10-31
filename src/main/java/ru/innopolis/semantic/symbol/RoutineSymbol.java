package ru.innopolis.semantic.symbol;

import ru.innopolis.ast.TypeNode;
import ru.innopolis.lexer.Span;

import java.util.List;

public class RoutineSymbol extends Symbol {
    private final List<ParameterSymbol> parameters;
    private final TypeNode returnTypeNode;
    private boolean isForward;
    private boolean isDefined;

    public RoutineSymbol(String name, Span position, List<ParameterSymbol> parameters,
                         TypeNode returnTypeNode, boolean isForward, boolean isDefined) {
        super(name, position);
        this.parameters = List.copyOf(parameters);
        this.returnTypeNode = returnTypeNode;
        this.isForward = isForward;
        this.isDefined = isDefined;
    }

    public List<ParameterSymbol> getParameters() {
        return parameters;
    }

    public TypeNode getReturnTypeNode() {
        return returnTypeNode;
    }

    public boolean isForward() {
        return isForward;
    }

    public void setForward(boolean forward) {
        isForward = forward;
    }

    public boolean isDefined() {
        return isDefined;
    }

    public void setDefined(boolean defined) {
        isDefined = defined;
    }
}

