package ru.innopolis.parser;

import ru.innopolis.ast.ASTNode;
import ru.innopolis.ast.Declaration;
import ru.innopolis.ast.Statement;
import ru.innopolis.ast.ProgramNode;
import ru.innopolis.ast.TypeNode;
import ru.innopolis.ast.declaration.*;
import ru.innopolis.ast.type.*;
import ru.innopolis.ast.expression.*;
import ru.innopolis.ast.statement.*;
import ru.innopolis.lexer.Span;
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

    private Token peekNext() {
        if (current + 1 >= tokens.size()) return tokens.get(tokens.size() - 1);
        return tokens.get(current + 1);
    }

    private boolean isBodyEnd() {
        return isEnd() || check(TokenType.END) || check(TokenType.ELSE);
    }

    private boolean isOnNewLine() {
        return peek().getPosition().lineNum() > previous().getPosition().lineNum();
    }

    private void consumeSeparator() {
        if (match(TokenType.SEMICOLON)) {
            if (isBodyEnd() || isOnNewLine()) {
                throw new ParseException("Expected statement after ';' at " + previous().getPosition());
            }
            return;
        }
        if (isBodyEnd() || isOnNewLine()) return;
        throw new ParseException("Expected new line or ';' at " + peek().getPosition());
    }

    private void consumeDeclarationSeparator() {
        if (match(TokenType.SEMICOLON)) {
            if (isBodyEnd()) {
                throw new ParseException("Expected declaration or statement after ';' at " + previous().getPosition());
            }
            return;
        }
        if (isBodyEnd() || isOnNewLine()) return;
        throw new ParseException("Expected new line or ';' at " + peek().getPosition());
    }

    public BodyNode parseBody() {
        Span start = peek().getPosition();
        List<ASTNode> items = new ArrayList<>();

        while (!isBodyEnd()) {
            if (check(TokenType.VAR) || check(TokenType.TYPE)) {
                items.add(parseSimpleDeclaration());
            } else {
                items.add(parseStatement());
            }
            consumeSeparator();
        }

        if (items.isEmpty()) return new BodyNode(start, items);
        return new BodyNode(Span.combine(start, previous().getPosition()), items);
    }

    public Declaration parseSimpleDeclaration() {
        if (check(TokenType.VAR)) return parseVariableDeclaration();
        if (check(TokenType.TYPE)) return parseTypeDeclaration();
        throw new ParseException("Expected 'var' or 'type' at " + peek().getPosition());
    }

    public ProgramNode parseProgram() {
        Span start = peek().getPosition();
        List<Declaration> declarations = new ArrayList<>();
        while (!isEnd()) {
            declarations.add(parseDeclaration());
            consumeDeclarationSeparator();
        }
        Span position = declarations.isEmpty() ? start : Span.combine(start, previous().getPosition());
        return new ProgramNode(position, declarations);
    }

    public Declaration parseDeclaration() {
        if (check(TokenType.ROUTINE)) return parseRoutineDeclaration();
        return parseSimpleDeclaration();
    }

    public VariableDeclarationNode parseVariableDeclaration() {
        Token start = consume(TokenType.VAR, "Expected 'var' at " + peek().getPosition());
        IdentifierToken name = (IdentifierToken) consume(TokenType.IDENTIFIER,
                "Expected variable name at " + peek().getPosition());
        TypeNode type = match(TokenType.COLON) ? parseType() : null;
        Expression initializer = match(TokenType.IS) ? parseExpression() : null;
        if (type == null && initializer == null) {
            throw new ParseException("Expected variable type or initializer at " + peek().getPosition());
        }
        return new VariableDeclarationNode(Span.combine(start.getPosition(), previous().getPosition()),
                name.getName(), type, initializer);
    }

    public TypeDeclarationNode parseTypeDeclaration() {
        Token start = consume(TokenType.TYPE, "Expected 'type' at " + peek().getPosition());
        IdentifierToken name = (IdentifierToken) consume(TokenType.IDENTIFIER,
                "Expected type name at " + peek().getPosition());
        consume(TokenType.IS, "Expected 'is' after type name at " + peek().getPosition());
        TypeNode type = parseType();
        return new TypeDeclarationNode(Span.combine(start.getPosition(), previous().getPosition()),
                name.getName(), type);
    }

    public TypeNode parseType() {
        if (match(TokenType.INTEGER, TokenType.REAL, TokenType.BOOLEAN, TokenType.STRING)) {
            Token token = previous();
            return new PrimitiveTypeNode(token.getPosition(), token.getType());
        }
        if (match(TokenType.IDENTIFIER)) {
            IdentifierToken token = (IdentifierToken) previous();
            return new NamedTypeNode(token.getPosition(), token.getName());
        }
        if (match(TokenType.ARRAY)) {
            Token start = previous();
            Expression size = null;
            if (match(TokenType.LBRACKET)) {
                size = parseExpression();
                consume(TokenType.RBRACKET, "Expected ']' after array size at " + peek().getPosition());
            }
            TypeNode elementType = parseType();
            return new ArrayTypeNode(Span.combine(start.getPosition(), previous().getPosition()),
                    size, elementType);
        }
        if (match(TokenType.RECORD)) {
            Token start = previous();
            List<VariableDeclarationNode> fields = new ArrayList<>();
            while (!check(TokenType.END) && !isEnd()) {
                fields.add(parseVariableDeclaration());
                consumeDeclarationSeparator();
            }
            Token end = consume(TokenType.END, "Expected 'end' after record at " + peek().getPosition());
            return new RecordTypeNode(Span.combine(start.getPosition(), end.getPosition()), fields);
        }
        throw new ParseException("Expected type at " + peek().getPosition());
    }

    public ParameterNode parseParameter() {
        IdentifierToken name = (IdentifierToken) consume(TokenType.IDENTIFIER,
                "Expected parameter name at " + peek().getPosition());
        consume(TokenType.COLON, "Expected ':' after parameter name at " + peek().getPosition());
        TypeNode type = parseType();
        return new ParameterNode(Span.combine(name.getPosition(), previous().getPosition()), name.getName(), type);
    }

    public RoutineDeclarationNode parseRoutineDeclaration() {
        Token start = consume(TokenType.ROUTINE, "Expected 'routine' at " + peek().getPosition());
        IdentifierToken name = (IdentifierToken) consume(TokenType.IDENTIFIER,
                "Expected routine name at " + peek().getPosition());
        consume(TokenType.LPAREN, "Expected '(' after routine name at " + peek().getPosition());
        List<ParameterNode> parameters = new ArrayList<>();
        if (!check(TokenType.RPAREN)) {
            do {
                parameters.add(parseParameter());
            } while (match(TokenType.COMMA));
        }
        consume(TokenType.RPAREN, "Expected ')' after parameters at " + peek().getPosition());
        TypeNode returnType = match(TokenType.COLON) ? parseType() : null;
        BodyNode body = null;
        Expression expressionBody = null;
        if (match(TokenType.EQUAL)) {
            Token equals = previous();
            Token greater = consume(TokenType.GREATER, "Expected '=>' after routine header at " + peek().getPosition());
            if (equals.getPosition().lineNum() != greater.getPosition().lineNum()
                    || equals.getPosition().posEnd() + 1 != greater.getPosition().posBegin()) {
                throw new ParseException("Expected adjacent '=' and '>' in '=>' at " + greater.getPosition());
            }
            expressionBody = parseExpression();
        } else if (match(TokenType.IS)) {
            body = parseBody();
            consume(TokenType.END, "Expected 'end' after routine body at " + peek().getPosition());
        }
        return new RoutineDeclarationNode(Span.combine(start.getPosition(), previous().getPosition()),
                name.getName(), parameters, returnType, body, expressionBody);
    }

    public Statement parseStatement() {
        switch (peek().getType()) {
            case IF: return parseIfStatement();
            case WHILE: return parseWhileLoop();
            case FOR: return parseForLoop();
            case PRINT: return parsePrintStatement();
            case RETURN: return parseReturnStatement();
            case IDENTIFIER:
                TokenType next = peekNext().getType();
                if (next == TokenType.ASSIGN || next == TokenType.DOT || next == TokenType.LBRACKET) {
                    return parseAssignment();
                }
                return parseRoutineCallStatement();
            default:
                throw new ParseException("Expected statement at " + peek().getPosition());
        }
    }

    public AssignmentNode parseAssignment() {
        if (!check(TokenType.IDENTIFIER)) {
            throw new ParseException("Expected variable name at " + peek().getPosition());
        }
        Expression target = parsePrimary();
        consume(TokenType.ASSIGN, "Expected ':=' after assignment target");
        Expression value = parseExpression();

        return new AssignmentNode(target, value);
    }

    public RoutineCallStatementNode parseRoutineCallStatement() {
        if (!check(TokenType.IDENTIFIER)) {
            throw new ParseException("Expected routine name at " + peek().getPosition());
        }
        if (peekNext().getType() == TokenType.LPAREN) {
            return new RoutineCallStatementNode((RoutineCallExpressionNode) parsePrimary());
        }

        IdentifierToken token = (IdentifierToken) step();
        return new RoutineCallStatementNode(
                new RoutineCallExpressionNode(token.getPosition(), token.getName(), new ArrayList<>()));
    }

    public IfStatementNode parseIfStatement() {
        Token keyword = consume(TokenType.IF, "Expected 'if'");
        Expression condition = parseExpression();
        consume(TokenType.THEN, "Expected 'then' after if condition");
        BodyNode thenBody = parseBody();

        BodyNode elseBody = null;
        if (match(TokenType.ELSE)) {
            elseBody = parseBody();
        }
        Token end = consume(TokenType.END, "Expected 'end' after if statement");

        return new IfStatementNode(Span.combine(keyword.getPosition(), end.getPosition()),
                condition, thenBody, elseBody);
    }

    public WhileLoopNode parseWhileLoop() {
        Token keyword = consume(TokenType.WHILE, "Expected 'while'");
        Expression condition = parseExpression();
        consume(TokenType.LOOP, "Expected 'loop' after while condition");
        BodyNode body = parseBody();
        Token end = consume(TokenType.END, "Expected 'end' after while loop");

        return new WhileLoopNode(Span.combine(keyword.getPosition(), end.getPosition()), condition, body);
    }

    public ForLoopNode parseForLoop() {
        Token keyword = consume(TokenType.FOR, "Expected 'for'");
        Token variable = consume(TokenType.IDENTIFIER, "Expected loop variable name after 'for'");
        consume(TokenType.IN, "Expected 'in' after loop variable");
        RangeNode range = parseRange();
        boolean reverse = match(TokenType.REVERSE);
        consume(TokenType.LOOP, "Expected 'loop' after for range");
        BodyNode body = parseBody();
        Token end = consume(TokenType.END, "Expected 'end' after for loop");

        return new ForLoopNode(Span.combine(keyword.getPosition(), end.getPosition()),
                ((IdentifierToken) variable).getName(), range, reverse, body);
    }

    public RangeNode parseRange() {
        Expression start = parseExpression();
        Expression end = null;
        if (match(TokenType.DOT_DOT)) {
            end = parseExpression();
        }

        return new RangeNode(start, end);
    }

    public PrintStatementNode parsePrintStatement() {
        Token keyword = consume(TokenType.PRINT, "Expected 'print'");
        List<Expression> expressions = new ArrayList<>();
        do {
            expressions.add(parseExpression());
        } while (match(TokenType.COMMA));

        return new PrintStatementNode(Span.combine(keyword.getPosition(), previous().getPosition()), expressions);
    }

    public ReturnStatementNode parseReturnStatement() {
        Token keyword = consume(TokenType.RETURN, "Expected 'return'");
        if (isBodyEnd() || isOnNewLine()) {
            return new ReturnStatementNode(keyword.getPosition(), null);
        }
        Expression value = parseExpression();

        return new ReturnStatementNode(Span.combine(keyword.getPosition(), previous().getPosition()), value);
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
