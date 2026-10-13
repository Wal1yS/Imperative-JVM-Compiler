package ru.innopolis.ast.statement;

import ru.innopolis.ast.Statement;
import ru.innopolis.lexer.Span;

public class ForLoopNode extends Statement {
    private final String variableName;
    private final RangeNode range;
    private final boolean reverse;
    private final BodyNode body;

    public ForLoopNode(Span position, String variableName, RangeNode range, boolean reverse, BodyNode body) {
        super(position);
        this.variableName = variableName;
        this.range = range;
        this.reverse = reverse;
        this.body = body;
    }

    public String getVariableName() {
        return variableName;
    }

    public RangeNode getRange() {
        return range;
    }

    public boolean isReverse() {
        return reverse;
    }

    public BodyNode getBody() {
        return body;
    }
}
