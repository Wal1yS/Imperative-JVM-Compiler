package ru.innopolis.ast.statement;

import ru.innopolis.ast.Statement;
import ru.innopolis.ast.expression.Expression;
import ru.innopolis.lexer.Span;

public class ReturnStatementNode extends Statement {
    private final Expression value;

    public ReturnStatementNode(Span position, Expression value) {
        super(position);
        this.value = value;
    }

    public Expression getValue() {
        return value;
    }
}
