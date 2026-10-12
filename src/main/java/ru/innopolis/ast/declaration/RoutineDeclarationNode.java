package ru.innopolis.ast.declaration;

import ru.innopolis.ast.Declaration;
import ru.innopolis.lexer.Span;
import ru.innopolis.ast.TypeNode;
import ru.innopolis.ast.expression.Expression;
import ru.innopolis.ast.statement.BodyNode;
import java.util.List;

/** Forward declarations have no body. A null returnType denotes an omitted result type. */
public class RoutineDeclarationNode extends Declaration {
    private final String name;
    private final List<ParameterNode> parameters;
    private final TypeNode returnType;
    private final BodyNode body;
    private final Expression expressionBody;

    public RoutineDeclarationNode(Span position, String name, List<ParameterNode> parameters, TypeNode returnType, BodyNode body, Expression expressionBody) {
        super(position);
        this.name = name;
        this.parameters = List.copyOf(parameters);
        this.returnType = returnType;
        this.body = body;
        this.expressionBody = expressionBody;
    }

    public String getName() {
        return name;
    }

    public List<ParameterNode> getParameters() {
        return parameters;
    }

    public TypeNode getReturnType() {
        return returnType;
    }

    public BodyNode getBody() {
        return body;
    }

    public Expression getExpressionBody() {
        return expressionBody;
    }

    public boolean isForwardDeclaration() {
        return body == null && expressionBody == null;
    }
}

