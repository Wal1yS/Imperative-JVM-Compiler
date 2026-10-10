package ru.innopolis.parser;

import ru.innopolis.ast.expression.*;
import ru.innopolis.lexer.token.*;
import ru.innopolis.lexer.TokenType;

import java.util.ArrayList;
import java.util.List;

public class Parser {
    private final List<Token> tokens;
    private int current = 0;

    public Parser(List<Token> tokens){
        this.tokens = tokens;
    }

    public Token peek(){
        return tokens.get(current);
    }

    public Token previous(){
        return tokens.get(current-1);
    }

    public boolean isEnd(){
        return peek().getType() == TokenType.EOF;
    }

    public boolean check(TokenType type){
        if (isEnd()){
            return false;
        }
        return peek().getType() == type;
    }

    public Token step(){
        if (!(isEnd())) current++;
        return previous();
    }

    public boolean match(TokenType... types) {
        for (TokenType type : types) {
            if (peek().getType() == type) {
                step();
                return true;
            }
        }
        return false;
    }

    public Token consume(TokenType type, String errorMessage) {
        if (check(type)) return step();
        throw new ParseException(errorMessage);
    }

    public Expression parsePrimary(){
        if (match(TokenType.MINUS, TokenType.PLUS, TokenType.NOT)) {
            Token operator = previous();
            Expression operand = parsePrimary();
            return new UnaryExpressionNode(operator.getPosition(), operand, operator.getType());
        }

        if (match(TokenType.INT_LITERAL)) {
            IntToken token = (IntToken) previous();
            return new IntLiteralNode(token.getPosition(), token.getValue());
        }

        if (match(TokenType.REAL_LITERAL)) {
            RealToken token = (RealToken) previous();
            return new RealLiteralNode(token.getPosition(), token.getValue());
        }

        if (match(TokenType.STRING_LITERAL)) {
            StringToken token = (StringToken) previous();
            return new StringLiteralNode(token.getPosition(), token.getValue());
        }

        if (match(TokenType.TRUE)) {
            Token token = previous();
            return new BoolLiteralNode(token.getPosition(), true);
        }

        if (match(TokenType.FALSE)) {
            Token token = previous();
            return new BoolLiteralNode(token.getPosition(), false);
        }

        if (match(TokenType.IDENTIFIER)) {
            IdentifierToken token = (IdentifierToken) previous();
            String name = token.getName();

            if (match(TokenType.LPAREN)) {
                List<Expression> arguments = new ArrayList<>();
                if (!check(TokenType.RPAREN)){
                    do {
                        arguments.add(parseExpression());
                    } while (match(TokenType.COMMA));
                }
                consume(TokenType.RPAREN, "Expected ')' after function arguments");
                return new RoutineCallExpressionNode(token.getPosition(), name, arguments);
            }

            Expression expression = new VariableReferenceNode(token.getPosition(), name);

            while (true) {
                if (match(TokenType.DOT)) {
                    Token fieldToken = consume(TokenType.IDENTIFIER, "Expected field name after '.'");
                    String fieldName = ((IdentifierToken) fieldToken).getName();

                    expression = new RecordAccessNode(token.getPosition(), expression, fieldName);
                } else if (match(TokenType.LBRACKET)) {
                    Expression index = parseExpression();
                    consume(TokenType.RBRACKET, "Expected ']' after arrays index");

                    expression = new ArrayAccessNode(token.getPosition(), expression, index);
                } else{
                    break;
                }
            }

            return expression;
        }

        throw new ParseException("Expected expression at " + peek().getPosition());
    }

    public Expression parseSummand() {
        if (match(TokenType.LPAREN)) {
            Expression expr = parseExpression();
            consume(TokenType.RPAREN, "Expected ')' after expression");
            return expr;
        }
        return parsePrimary();
    }

    public Expression parseFactor() {
        Expression left = parseSummand();

        while (match(TokenType.MULT, TokenType.DIV, TokenType.MOD)) {
            Token operator = previous();
            Expression right = parseSummand();

            left = new BinaryExpressionNode(left, operator.getType(), right);
        }

        return left;
    }

    public Expression parseSimple() {
        Expression left = parseFactor();

        while (match(TokenType.PLUS, TokenType.MINUS)) {
            Token operator = previous();
            Expression right = parseFactor();

            left = new BinaryExpressionNode(left, operator.getType(), right);
        }

        return left;
    }

    public Expression parseRelation() {
        Expression left = parseSimple();

        while (match(TokenType.LESS, TokenType.LESS_EQUAL, TokenType.GREATER,
                TokenType.GREATER_EQUAL, TokenType.EQUAL, TokenType.NOT_EQUAL)) {
            Token operator = previous();
            Expression right = parseSimple();

            left = new BinaryExpressionNode(left, operator.getType(), right);
        }

        return left;
    }

    public Expression parseExpression() {
        Expression left = parseRelation();

        while (match(TokenType.AND, TokenType.OR, TokenType.XOR)) {
            Token operator = previous();
            Expression right = parseRelation();

            left = new BinaryExpressionNode(left, operator.getType(), right);
        }

        return left;
    }
}
