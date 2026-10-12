package ru.innopolis.ast;

import ru.innopolis.lexer.Span;
import java.util.List;

public class ProgramNode extends ASTNode {
    private final List<Declaration> declarations;

    public ProgramNode(Span position, List<Declaration> declarations) {
        super(position);
        this.declarations = List.copyOf(declarations);
    }

    public List<Declaration> getDeclarations() {
        return declarations;
    }
}

