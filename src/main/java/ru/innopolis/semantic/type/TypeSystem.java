package ru.innopolis.semantic.type;

import ru.innopolis.lexer.TokenType;

import java.util.Objects;

import static ru.innopolis.semantic.type.PrimitiveType.BOOLEAN;
import static ru.innopolis.semantic.type.PrimitiveType.INTEGER;
import static ru.innopolis.semantic.type.PrimitiveType.REAL;

public final class TypeSystem {
    private TypeSystem() {
    }

    public static boolean isAssignable(Type target, Type source) {
        if (target.isError() || source.isError()) return true;
        if (target == source) return true;
        if (target instanceof ArrayType t && source instanceof ArrayType s) {
            return sameType(t.getElementType(), s.getElementType())
                    && (t.getSize() == null || s.getSize() == null || t.getSize().equals(s.getSize()));
        }
        return false;
    }

    public static boolean sameType(Type a, Type b) {
        if (a.isError() || b.isError()) return true;
        if (a == b) return true;
        if (a instanceof ArrayType x && b instanceof ArrayType y) {
            return x.isSized() == y.isSized()
                    && Objects.equals(x.getSize(), y.getSize())
                    && sameType(x.getElementType(), y.getElementType());
        }
        return false;
    }

    public static boolean canCast(Type from, Type to) {
        if (from.isError() || to.isError()) return true;
        if (from == to) return from instanceof PrimitiveType;
        if (!isScalar(from) || !isScalar(to)) return false;
        return !(from == REAL && to == BOOLEAN);
    }

    public static Type binaryResult(TokenType op, Type left, Type right) {
        if (left.isError() || right.isError()) return ErrorType.INSTANCE;
            if (left != right) return null;
        switch (op) {
            case PLUS, MINUS, MULT, DIV -> {
                return isNumeric(left) ? left : null;
            }
            case MOD -> {
                return left == INTEGER ? INTEGER : null;
            }
            case AND, OR, XOR -> {
                return left == BOOLEAN ? BOOLEAN : null;
            }
            case LESS, LESS_EQUAL, GREATER, GREATER_EQUAL -> {
                return isNumeric(left) ? BOOLEAN : null;
            }
            case EQUAL, NOT_EQUAL -> {
                return left instanceof PrimitiveType ? BOOLEAN : null;
            }
            default -> {
                return null;
            }
        }
    }

    public static Type unaryResult(TokenType op, Type operand) {
        if (operand.isError()) return ErrorType.INSTANCE;
        switch (op) {
            case PLUS, MINUS -> {
                return isNumeric(operand) ? operand : null;
            }
            case NOT -> {
                return operand == BOOLEAN ? BOOLEAN : null;
            }
            default -> {
                return null;
            }
        }
    }

    public static boolean isPrintable(Type t) {
        return t.isError() || t instanceof PrimitiveType;
    }

    public static boolean isNumeric(Type t) {
        return t instanceof PrimitiveType p && p.isNumeric();
    }

    private static boolean isScalar(Type t) {
        return t == INTEGER || t == REAL || t == BOOLEAN;
    }

    public static String operatorSymbol(TokenType op) {
        return switch (op) {
            case PLUS -> "+";
            case MINUS -> "-";
            case MULT -> "*";
            case DIV -> "/";
            case MOD -> "%";
            case AND -> "and";
            case OR -> "or";
            case XOR -> "xor";
            case NOT -> "not";
            case LESS -> "<";
            case LESS_EQUAL -> "<=";
            case GREATER -> ">";
            case GREATER_EQUAL -> ">=";
            case EQUAL -> "=";
            case NOT_EQUAL -> "/=";
            default -> op.name().toLowerCase();
        };
    }
}
