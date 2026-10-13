package ru.innopolis.ast.expression;

import ru.innopolis.lexer.Span;

import java.util.List;

public class RoutineCallExpressionNode extends Expression{
    private final String routineName;
    private final List<Expression> arguments;

    public RoutineCallExpressionNode(Span position, String routineName, List<Expression> arguments){
        super(position);
        this.routineName = routineName;
        this.arguments = List.copyOf(arguments);
    }

    public String getRoutineName() {
        return routineName;
    }

    public List<Expression> getArguments() {
        return arguments;
    }
}
