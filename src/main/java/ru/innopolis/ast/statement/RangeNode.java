package ru.innopolis.ast.statement;

import ru.innopolis.ast.ASTNode;
import ru.innopolis.ast.expression.Expression;
import ru.innopolis.lexer.Span;

public class RangeNode extends ASTNode {
    private final Expression start;
    private final Expression end;

    public RangeNode(Expression start, Expression end) {
        super(end == null ? start.getPosition() : Span.combine(start.getPosition(), end.getPosition()));
        this.start = start;
        this.end = end;
    }

    public Expression getStart() {
        return start;
    }

    public Expression getEnd() {
        return end;
    }

    public boolean isArrayIteration() {
        return end == null;
    }
}
