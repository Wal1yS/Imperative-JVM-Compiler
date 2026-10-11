package ru.innopolis.ast.statement;

import ru.innopolis.ast.Statement;
import ru.innopolis.ast.expression.Expression;
import ru.innopolis.lexer.Span;

public class IfStatementNode extends Statement {
    private final Expression condition;
    private final BodyNode thenBody;
    private final BodyNode elseBody;

    public IfStatementNode(Span position, Expression condition, BodyNode thenBody, BodyNode elseBody) {
        super(position);
        this.condition = condition;
        this.thenBody = thenBody;
        this.elseBody = elseBody;
    }

    public Expression getCondition() {
        return condition;
    }

    public BodyNode getThenBody() {
        return thenBody;
    }

    public BodyNode getElseBody() {
        return elseBody;
    }
}
