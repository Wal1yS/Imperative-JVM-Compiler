package ru.innopolis.ast.type;

import ru.innopolis.ast.TypeNode;
import ru.innopolis.lexer.Span;
import ru.innopolis.ast.expression.Expression;

/** A null size represents an array whose length is not specified in the type. */
public class ArrayTypeNode extends TypeNode {
    private final Expression size;
    private final TypeNode elementType;

    public ArrayTypeNode(Span position, Expression size, TypeNode elementType) {
        super(position);
        this.size = size;
        this.elementType = elementType;
    }

    public Expression getSize() {
        return size;
    }

    public TypeNode getElementType() {
        return elementType;
    }
}

