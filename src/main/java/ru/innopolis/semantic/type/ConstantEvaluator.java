package ru.innopolis.semantic.type;

import ru.innopolis.ast.expression.*;
import ru.innopolis.ast.type.PrimitiveTypeNode;

public final class ConstantEvaluator {
    private ConstantEvaluator() {
    }

    public static Object evaluate(Expression expr) {
        if (expr instanceof IntLiteralNode i) return i.getValue();
        if (expr instanceof RealLiteralNode r) return r.getValue();
        if (expr instanceof BoolLiteralNode b) return b.getValue();
        if (expr instanceof StringLiteralNode s) return s.getValue();
        if (expr instanceof UnaryExpressionNode u) return evaluateUnary(u);
        if (expr instanceof BinaryExpressionNode b) return evaluateBinary(b);
        if (expr instanceof CastExpressionNode c) return evaluateCast(c);
        return null;
    }

    public static Integer evaluateInt(Expression expr) {
        return evaluate(expr) instanceof Integer i ? i : null;
    }

    private static Object evaluateUnary(UnaryExpressionNode u) {
        Object v = evaluate(u.getOperand());
        if (v == null) return null;
        return switch (u.getOperator()) {
            case PLUS -> (v instanceof Integer || v instanceof Double) ? v : null;
            case MINUS -> v instanceof Integer i ? (Object) (-i) : v instanceof Double d ? (Object) (-d) : null;
            case NOT -> v instanceof Boolean b ? (Object) (!b) : null;
            default -> null;
        };
    }

    private static Object evaluateBinary(BinaryExpressionNode b) {
        Object l = evaluate(b.getLeft());
        Object r = evaluate(b.getRight());
        if (l == null || r == null) return null;

        if (l instanceof Boolean x && r instanceof Boolean y) {
            return switch (b.getOperator()) {
                case AND -> x && y;
                case OR -> x || y;
                case XOR -> x ^ y;
                case EQUAL -> x.equals(y);
                case NOT_EQUAL -> !x.equals(y);
                default -> null;
            };
        }
        if (l instanceof Integer x && r instanceof Integer y) {
            return switch (b.getOperator()) {
                case PLUS -> x + y;
                case MINUS -> x - y;
                case MULT -> x * y;
                case DIV -> y == 0 ? null : (Object) (x / y);
                case MOD -> y == 0 ? null : (Object) (x % y);
                case LESS -> x < y;
                case LESS_EQUAL -> x <= y;
                case GREATER -> x > y;
                case GREATER_EQUAL -> x >= y;
                case EQUAL -> x.equals(y);
                case NOT_EQUAL -> !x.equals(y);
                default -> null;
            };
        }
        if (isNumber(l) && isNumber(r)) {
            double x = ((Number) l).doubleValue();
            double y = ((Number) r).doubleValue();
            return switch (b.getOperator()) {
                case PLUS -> x + y;
                case MINUS -> x - y;
                case MULT -> x * y;
                case DIV -> y == 0 ? null : (Object) (x / y);
                case LESS -> x < y;
                case LESS_EQUAL -> x <= y;
                case GREATER -> x > y;
                case GREATER_EQUAL -> x >= y;
                case EQUAL -> x == y;
                case NOT_EQUAL -> x != y;
                default -> null;
            };
        }
        return null;
    }

    private static Object evaluateCast(CastExpressionNode c) {
        Object v = evaluate(c.getExpression());
        if (v == null || !(c.getTargetType() instanceof PrimitiveTypeNode target)) return null;
        return switch (target.getKind()) {
            case INTEGER -> v instanceof Integer ? v
                    : v instanceof Double d ? (Object) (int) Math.round(d)
                    : v instanceof Boolean b ? (Object) (b ? 1 : 0) : null;
            case REAL -> v instanceof Double ? v
                    : v instanceof Integer i ? (Object) i.doubleValue()
                    : v instanceof Boolean b ? (Object) (b ? 1.0 : 0.0) : null;
            case BOOLEAN -> v instanceof Boolean ? v
                    : v instanceof Integer i && (i == 0 || i == 1) ? (Object) (i == 1) : null;
            case STRING -> v instanceof String ? v : null;
            default -> null;
        };
    }

    private static boolean isNumber(Object o) {
        return o instanceof Integer || o instanceof Double;
    }
}
