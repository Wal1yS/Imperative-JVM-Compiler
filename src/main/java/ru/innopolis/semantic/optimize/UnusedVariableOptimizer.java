package ru.innopolis.semantic.optimize;

import ru.innopolis.ast.ASTNode;
import ru.innopolis.ast.ProgramNode;
import ru.innopolis.ast.declaration.RoutineDeclarationNode;
import ru.innopolis.ast.declaration.VariableDeclarationNode;
import ru.innopolis.ast.expression.*;
import ru.innopolis.ast.statement.AssignmentNode;
import ru.innopolis.ast.statement.BodyNode;
import ru.innopolis.semantic.context.SemanticContext;
import ru.innopolis.semantic.symbol.Symbol;
import ru.innopolis.semantic.symbol.ParameterSymbol;
import ru.innopolis.semantic.symbol.VariableSymbol;
import ru.innopolis.semantic.type.PrimitiveType;
import ru.innopolis.semantic.visitor.ASTTransformer;
import ru.innopolis.semantic.visitor.BaseASTVisitor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

/**
 * Removes unread and unwritten scalar locals with safely discardable initialization.
 * Calls, possible runtime errors and aggregate allocations are retained. Existing readCount
 * values are not used because earlier passes remove code.
 */
public class UnusedVariableOptimizer extends ASTTransformer {
    private final Set<Symbol> reads = Collections.newSetFromMap(new IdentityHashMap<>());
    private final Set<Symbol> writes = Collections.newSetFromMap(new IdentityHashMap<>());
    private final Set<Symbol> initializedLocals = Collections.newSetFromMap(new IdentityHashMap<>());
    private int routineDepth;
    private boolean visitingProgram;

    public UnusedVariableOptimizer(SemanticContext context) {
        super(context);
    }

    @Override
    public ASTNode visit(ProgramNode node, Void unused) {
        reads.clear();
        writes.clear();
        initializedLocals.clear();
        routineDepth = 0;
        node.accept(new UsageCollector(), null);
        visitingProgram = true;
        try {
            return super.visit(node, unused);
        } finally {
            visitingProgram = false;
        }
    }

    @Override
    public ASTNode visit(RoutineDeclarationNode node, Void unused) {
        if (!visitingProgram) {
            throw new IllegalArgumentException("Unused-variable analysis must start at the program root");
        }
        routineDepth++;
        try {
            return super.visit(node, unused);
        } finally {
            routineDepth--;
        }
    }

    @Override
    public ASTNode visit(BodyNode node, Void unused) {
        for (ASTNode item : node.getItems()) {
            if (item instanceof VariableDeclarationNode variable && variable.getInitializer() != null) {
                Symbol symbol = context.getDeclaredSymbol(variable);
                if (symbol != null) initializedLocals.add(symbol);
            }
        }
        List<ASTNode> items = new ArrayList<>();
        boolean changed = false;
        for (ASTNode item : node.getItems()) {
            ASTNode replacement = transform(item);
            if (routineDepth > 0 && replacement instanceof VariableDeclarationNode declaration
                    && removable(declaration)) {
                replacement = null;
            }
            changed |= replacement != item;
            if (replacement != null) items.add(replacement);
        }
        return changed ? new BodyNode(node.getPosition(), items) : node;
    }

    private boolean removable(VariableDeclarationNode declaration) {
        Symbol symbol = context.getDeclaredSymbol(declaration);
        if (!(symbol instanceof VariableSymbol) || reads.contains(symbol) || writes.contains(symbol)) return false;
        if (!(context.getSymbolType(symbol) instanceof PrimitiveType)) return false;
        Expression initializer = declaration.getInitializer();
        if (initializer == null || initializer instanceof IntLiteralNode || initializer instanceof RealLiteralNode
                || initializer instanceof BoolLiteralNode || initializer instanceof StringLiteralNode) return true;
        if (initializer instanceof VariableReferenceNode reference) {
            Symbol source = context.getResolvedSymbol(reference);
            return source instanceof ParameterSymbol || initializedLocals.contains(source)
                    || source instanceof VariableSymbol variable && variable.isReadOnly();
        }
        return false;
    }

    private class UsageCollector extends BaseASTVisitor<Void, Void> {
        @Override
        public Void visit(VariableReferenceNode node, Void unused) {
            Symbol symbol = context.getResolvedSymbol(node);
            if (symbol != null) reads.add(symbol);
            return null;
        }

        @Override
        public Void visit(AssignmentNode node, Void unused) {
            if (node.getTarget() instanceof VariableReferenceNode reference) {
                Symbol symbol = context.getResolvedSymbol(reference);
                if (symbol != null) writes.add(symbol);
            } else {
                // Updating a field/element reads the owning reference and the index expressions.
                node.getTarget().accept(this, null);
            }
            node.getValue().accept(this, null);
            return null;
        }
    }
}
