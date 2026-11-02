package ru.innopolis.semantic.check;

import ru.innopolis.ast.expression.*;
import ru.innopolis.ast.statement.ForLoopNode;
import ru.innopolis.ast.statement.IfStatementNode;
import ru.innopolis.ast.statement.RangeNode;
import ru.innopolis.ast.statement.WhileLoopNode;
import ru.innopolis.semantic.context.SemanticContext;
import ru.innopolis.semantic.symbol.Symbol;
import ru.innopolis.semantic.type.ArrayType;
import ru.innopolis.semantic.type.ConstantEvaluator;
import ru.innopolis.semantic.visitor.BaseASTVisitor;

import java.util.HashMap;
import java.util.Map;

/**
 * Pass 3: Static Array Bounds Checker. Runs after the type checker and uses its annotations.
 * Does not modify the AST.
 * Implements:
 * - Curator Check 4: Array bound checking where possible, 0 <= i < size(arr)
 * - Vitebsk Check 6: arrays are 0-based, so the valid indices are 0 .. size - 1
 *
 * An index is checked when its value range is known at compile time: a constant expression,
 * or an expression of 'for' loop variables with constant ranges (e.g. 'arr[i + 1]' inside
 * 'for i in 0 .. 9').
 */
public class ArrayBoundsChecker extends BaseASTVisitor<Void, Void> {
    private final SemanticContext context;
    /** Value ranges [min, max] of loop variables of the enclosing 'for' loops with constant bounds. */
    private final Map<Symbol, long[]> loopRanges = new HashMap<>();

    public ArrayBoundsChecker(SemanticContext context) {
        this.context = context;
    }

    @Override
    public Void visit(ForLoopNode node, Void unused) {
        RangeNode range = node.getRange();
        range.accept(this, null);

        Symbol loopVar = context.getDeclaredSymbol(node);
        Integer from = range.isArrayIteration() ? null : ConstantEvaluator.evaluateInt(range.getStart());
        Integer to = range.isArrayIteration() ? null : ConstantEvaluator.evaluateInt(range.getEnd());
        // An empty range never executes the body, so nothing inside can fail
        boolean known = loopVar != null && from != null && to != null && from <= to;
        if (known) {
            loopRanges.put(loopVar, new long[]{from, to});
        }
        node.getBody().accept(this, null);
        if (known) {
            loopRanges.remove(loopVar);
        }
        return null;
    }

    @Override
    public Void visit(IfStatementNode node, Void unused) {
        node.getCondition().accept(this, null);
        // Inside a branch the index may be guarded by the condition (e.g. 'if i > 0 then a[i - 1]'),
        // so loop ranges are not trusted there; constant indices are still checked
        withoutLoopRanges(() -> {
            node.getThenBody().accept(this, null);
            if (node.getElseBody() != null) node.getElseBody().accept(this, null);
        });
        return null;
    }

    @Override
    public Void visit(WhileLoopNode node, Void unused) {
        node.getCondition().accept(this, null);
        withoutLoopRanges(() -> node.getBody().accept(this, null));
        return null;
    }

    private void withoutLoopRanges(Runnable action) {
        Map<Symbol, long[]> saved = new HashMap<>(loopRanges);
        loopRanges.clear();
        action.run();
        loopRanges.putAll(saved);
    }

    @Override
    public Void visit(ArrayAccessNode node, Void unused) {
        node.getArray().accept(this, null);
        node.getIndex().accept(this, null);

        long[] range = indexRange(node.getIndex());
        if (range == null) {
            return null;
        }
        Integer size = context.getExpressionType(node.getArray()) instanceof ArrayType array ? array.getSize() : null;
        boolean constant = range[0] == range[1];

        if (range[0] < 0 || (size != null && range[1] >= size)) {
            String bounds = size != null ? "valid indices are 0.." + (size - 1) : "indices start at 0";
            if (constant) {
                context.addError(node.getIndex().getPosition(), "Array index out of bounds at compile time: index is "
                        + range[0] + (size != null ? ", array size is " + size : "") + " (" + bounds + ")");
            } else {
                context.addError(node.getIndex().getPosition(), "Array index out of bounds at compile time: index takes values "
                        + range[0] + ".." + range[1] + (size != null ? ", array size is " + size : "") + " (" + bounds + ")");
            }
        }
        return null;
    }

    /** Range [min, max] of an integer index expression, or null if unknown at compile time. */
    private long[] indexRange(Expression expr) {
        Integer constant = ConstantEvaluator.evaluateInt(expr);
        if (constant != null) {
            return new long[]{constant, constant};
        }
        if (expr instanceof VariableReferenceNode ref) {
            Symbol sym = context.getResolvedSymbol(ref);
            return sym != null ? loopRanges.get(sym) : null;
        }
        if (expr instanceof BinaryExpressionNode bin) {
            long[] l = indexRange(bin.getLeft());
            long[] r = indexRange(bin.getRight());
            if (l == null || r == null) return null;
            return switch (bin.getOperator()) {
                case PLUS -> new long[]{l[0] + r[0], l[1] + r[1]};
                case MINUS -> new long[]{l[0] - r[1], l[1] - r[0]};
                case MULT -> {
                    long a = l[0] * r[0], b = l[0] * r[1], c = l[1] * r[0], d = l[1] * r[1];
                    yield new long[]{Math.min(Math.min(a, b), Math.min(c, d)), Math.max(Math.max(a, b), Math.max(c, d))};
                }
                default -> null;
            };
        }
        return null;
    }
}
