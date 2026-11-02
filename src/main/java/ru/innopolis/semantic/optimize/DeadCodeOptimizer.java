package ru.innopolis.semantic.optimize;

import ru.innopolis.ast.ASTNode;
import ru.innopolis.ast.expression.BoolLiteralNode;
import ru.innopolis.ast.statement.*;
import ru.innopolis.semantic.context.SemanticContext;
import ru.innopolis.semantic.visitor.ASTTransformer;

import java.util.ArrayList;
import java.util.List;

/** Removes literal dead branches/loops and unreachable tails from an analyzed tree. */
public class DeadCodeOptimizer extends ASTTransformer {
    public DeadCodeOptimizer(SemanticContext context) {
        super(context);
    }

    @Override
    public ASTNode visit(IfStatementNode node, Void unused) {
        if (node.getCondition() instanceof BoolLiteralNode condition) {
            // Keep the chosen BodyNode and its own scope; never splice its declarations into the parent.
            return transformBody(condition.getValue() ? node.getThenBody() : node.getElseBody());
        }
        return super.visit(node, unused);
    }

    @Override
    public ASTNode visit(WhileLoopNode node, Void unused) {
        if (node.getCondition() instanceof BoolLiteralNode condition && !condition.getValue()) return null;
        return super.visit(node, unused);
    }

    @Override
    public ASTNode visit(BodyNode node, Void unused) {
        List<ASTNode> items = new ArrayList<>();
        boolean changed = false;
        for (ASTNode item : node.getItems()) {
            ASTNode replacement = transform(item);
            changed |= replacement != item;
            if (replacement == null) continue;
            items.add(replacement);
            if (definitelyReturns(replacement)) break;
        }
        changed |= items.size() != node.getItems().size();
        return changed ? new BodyNode(node.getPosition(), items) : node;
    }

    private boolean definitelyReturns(ASTNode node) {
        if (node instanceof ReturnStatementNode) return true;
        if (node instanceof BodyNode body) {
            for (ASTNode item : body.getItems()) {
                if (definitelyReturns(item)) return true;
            }
        }
        if (node instanceof IfStatementNode branch && branch.getElseBody() != null) {
            return definitelyReturns(branch.getThenBody()) && definitelyReturns(branch.getElseBody());
        }
        // A for/while body may never execute, so a return inside it alone is insufficient.
        return false;
    }
}
