package ru.innopolis.ast.statement;

import ru.innopolis.ast.Statement;
import ru.innopolis.ast.expression.RoutineCallExpressionNode;
import ru.innopolis.semantic.visitor.ASTVisitor;

public class RoutineCallStatementNode extends Statement {
    private final RoutineCallExpressionNode call;

    public RoutineCallStatementNode(RoutineCallExpressionNode call) {
        super(call.getPosition());
        this.call = call;
    }

    public RoutineCallExpressionNode getCall() {
        return call;
    }

    @Override
    public <R, C> R accept(ASTVisitor<R, C> visitor, C context) {
        return visitor.visit(this, context);
    }
}
