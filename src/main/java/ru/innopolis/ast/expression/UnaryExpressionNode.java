package ru.innopolis.ast.expression;

import ru.innopolis.lexer.Span;
import ru.innopolis.lexer.TokenType;

public class UnaryExpressionNode extends Expression {
    private final TokenType operator;
    private final Expression operand;

    public UnaryExpressionNode(Span position, Expression operand, TokenType type) {
        super(position);
        this.operand = operand;
        this.operator = type;
    }

    public Expression getOperand() {
        return operand;
    }

    public TokenType getOperator() {
        return operator;
    }
}
