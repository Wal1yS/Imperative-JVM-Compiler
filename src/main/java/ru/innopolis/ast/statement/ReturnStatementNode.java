package ru.innopolis.ast.statement;

import ru.innopolis.ast.Statement;
import ru.innopolis.ast.expression.Expression;
import ru.innopolis.lexer.Span;
import ru.innopolis.semantic.visitor.ASTVisitor;

public class ReturnStatementNode extends Statement {
    private final Expression value;

    public ReturnStatementNode(Span position, Expression value) {
        super(position);
        this.value = value;
    }

    public Expression getValue() {
        return value;
    }

    @Override
    public <R, C> R accept(ASTVisitor<R, C> visitor, C context) {
        return visitor.visit(this, context);
    }
}
