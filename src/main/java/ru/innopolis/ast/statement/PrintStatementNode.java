package ru.innopolis.ast.statement;

import ru.innopolis.ast.Statement;
import ru.innopolis.ast.expression.Expression;
import ru.innopolis.lexer.Span;

import java.util.List;

public class PrintStatementNode extends Statement {
    private final List<Expression> expressions;

    public PrintStatementNode(Span position, List<Expression> expressions) {
        super(position);
        this.expressions = expressions;
    }

    public List<Expression> getExpressions() {
        return expressions;
    }
}
