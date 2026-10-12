package ru.innopolis.ast.type;

import ru.innopolis.ast.TypeNode;
import ru.innopolis.lexer.Span;
import ru.innopolis.ast.declaration.VariableDeclarationNode;
import java.util.List;

public class RecordTypeNode extends TypeNode {
    private final List<VariableDeclarationNode> fields;

    public RecordTypeNode(Span position, List<VariableDeclarationNode> fields) {
        super(position);
        this.fields = List.copyOf(fields);
    }

    public List<VariableDeclarationNode> getFields() {
        return fields;
    }
}

