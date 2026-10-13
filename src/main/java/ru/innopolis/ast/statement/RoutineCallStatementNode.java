package ru.innopolis.ast.statement;

import ru.innopolis.ast.Statement;
import ru.innopolis.ast.expression.RoutineCallExpressionNode;

public class RoutineCallStatementNode extends Statement {
    private final RoutineCallExpressionNode call;

    public RoutineCallStatementNode(RoutineCallExpressionNode call) {
        super(call.getPosition());
        this.call = call;
    }

    public RoutineCallExpressionNode getCall() {
        return call;
    }
}
