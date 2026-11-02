package ru.innopolis.semantic.type;

import ru.innopolis.lexer.TokenType;

import java.util.Objects;

import static ru.innopolis.semantic.type.PrimitiveType.BOOLEAN;
import static ru.innopolis.semantic.type.PrimitiveType.INTEGER;
import static ru.innopolis.semantic.type.PrimitiveType.REAL;
import static ru.innopolis.semantic.type.PrimitiveType.STRING;

/**
 * Type rules of the Vitebsk dialect of Language I.
 * No implicit conversions except widening integer to real; everything else goes through 'as'.
 */
public final class TypeSystem {

    private TypeSystem() {
    }

    /**
     * Whether a value of type {@code source} may be stored into a location of type {@code target}.
     * Used for assignments, initializers, argument passing and return values.
     */
    public static boolean isAssignable(Type target, Type source) {
        if (target.isError() || source.isError()) return true;
        if (target == source) return true;
        // The only implicit conversion: integer widens to real
        if (target == REAL && source == INTEGER) return true;
        if (target instanceof ArrayType t && source instanceof ArrayType s) {
            // Sizes are checked only when both are known at compile time
            return sameType(t.getElementType(), s.getElementType())
                    && (t.getSize() == null || s.getSize() == null || t.getSize().equals(s.getSize()));
        }
        // Records: identity only (checked above)
        return false;
    }

    /** Exact type identity, used for array elements and forward declaration signatures. */
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

    /**
     * Explicit 'as' conversions follow the conformance table of the language:
     * integer <-> real, boolean -> integer, boolean -> real, integer -> boolean.
     * real -> boolean is illegal; strings and user-defined types are never converted.
     */
    public static boolean canCast(Type from, Type to) {
        if (from.isError() || to.isError()) return true;
        if (from == to) return from instanceof PrimitiveType;
        if (!isScalar(from) || !isScalar(to)) return false;
        return !(from == REAL && to == BOOLEAN);
    }

    /** Result type of a binary operator, or null if the operands are not allowed. */
    public static Type binaryResult(TokenType op, Type left, Type right) {
        if (left.isError() || right.isError()) return ErrorType.INSTANCE;
        switch (op) {
            case PLUS, MINUS, MULT, DIV -> {
                if (isNumeric(left) && isNumeric(right)) {
                    return (left == INTEGER && right == INTEGER) ? INTEGER : REAL;
                }
                return null;
            }
            case MOD -> {
                return (left == INTEGER && right == INTEGER) ? INTEGER : null;
            }
            case AND, OR, XOR -> {
                return (left == BOOLEAN && right == BOOLEAN) ? BOOLEAN : null;
            }
            case LESS, LESS_EQUAL, GREATER, GREATER_EQUAL -> {
                return (isNumeric(left) && isNumeric(right)) ? BOOLEAN : null;
            }
            case EQUAL, NOT_EQUAL -> {
                if (isNumeric(left) && isNumeric(right)) return BOOLEAN;
                if (left == right && (left == BOOLEAN || left == STRING)) return BOOLEAN;
                return null;
            }
            default -> {
                return null;
            }
        }
    }

    /** Result type of a unary operator, or null if the operand is not allowed. */
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

    /** Only primitive values (including strings) can be printed. */
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
