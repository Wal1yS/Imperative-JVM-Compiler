package ru.innopolis.ast.declaration;

import ru.innopolis.ast.Declaration;
import ru.innopolis.lexer.Span;
import ru.innopolis.ast.TypeNode;
import ru.innopolis.ast.expression.Expression;

public class VariableDeclarationNode extends Declaration {
    private final String name;
    private final TypeNode type;
    private final Expression initializer;

    public VariableDeclarationNode(Span position, String name, TypeNode type, Expression initializer) {
        super(position);
        this.name = name;
        this.type = type;
        this.initializer = initializer;
    }

    public String getName() {
        return name;
    }

    public TypeNode getType() {
        return type;
    }

    public Expression getInitializer() {
        return initializer;
    }
}

