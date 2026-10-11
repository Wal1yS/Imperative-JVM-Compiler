package ru.innopolis.ast.statement;

import ru.innopolis.ast.Statement;
import ru.innopolis.ast.expression.Expression;
import ru.innopolis.lexer.Span;

public class WhileLoopNode extends Statement {
    private final Expression condition;
    private final BodyNode body;

    public WhileLoopNode(Span position, Expression condition, BodyNode body) {
        super(position);
        this.condition = condition;
        this.body = body;
    }

    public Expression getCondition() {
        return condition;
    }

    public BodyNode getBody() {
        return body;
    }
}
