package ru.innopolis.ast.statement;

import ru.innopolis.ast.Statement;
import ru.innopolis.ast.expression.Expression;
import ru.innopolis.lexer.Span;

public class AssignmentNode extends Statement {
    private final Expression target;
    private final Expression value;

    public AssignmentNode(Expression target, Expression value) {
        super(Span.combine(target.getPosition(), value.getPosition()));
        this.target = target;
        this.value = value;
    }

    public Expression getTarget() {
        return target;
    }

    public Expression getValue() {
        return value;
    }
}
