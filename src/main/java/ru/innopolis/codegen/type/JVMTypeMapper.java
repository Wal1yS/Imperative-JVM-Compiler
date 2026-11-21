package ru.innopolis.codegen.type;

import ru.innopolis.semantic.type.*;

import java.util.List;

/**
 * Maps Imperative Language semantic types to JVM descriptors and internal names.
 * Part of Person 1 (Valerii Tiniakov) foundational infrastructure.
 */
public final class JVMTypeMapper {

    private JVMTypeMapper() {}

    /**
     * Converts a semantic Type to its JVM bytecode type descriptor.
     * Examples: integer -> "I", real -> "D", boolean -> "Z", string -> "Ljava/lang/String;",
     * array [5] integer -> "[I", record Point -> "LCompiledProgram$Record_Point;"
     */
    public static String toDescriptor(Type type, String programClassName) {
        if (type == null || type instanceof VoidType) {
            return "V";
        }
        if (type == PrimitiveType.INTEGER) {
            return "I";
        }
        if (type == PrimitiveType.REAL) {
            return "D";
        }
        if (type == PrimitiveType.BOOLEAN) {
            return "Z";
        }
        if (type == PrimitiveType.STRING) {
            return "Ljava/lang/String;";
        }
        if (type instanceof ArrayType arrayType) {
            return "[" + toDescriptor(arrayType.getElementType(), programClassName);
        }
        if (type instanceof RecordType recordType) {
            return "L" + toRecordInternalName(recordType.getName(), programClassName) + ";";
        }
        return "Ljava/lang/Object;";
    }

    /**
     * Converts a reference type to its JVM internal class name (e.g. for NEW, ANEWARRAY, CHECKCAST).
     */
    public static String toInternalName(Type type, String programClassName) {
        if (type == PrimitiveType.STRING) {
            return "java/lang/String";
        }
        if (type instanceof ArrayType arrayType) {
            return toDescriptor(arrayType, programClassName);
        }
        if (type instanceof RecordType recordType) {
            return toRecordInternalName(recordType.getName(), programClassName);
        }
        return "java/lang/Object";
    }

    /**
     * Returns the JVM internal name for a record auxiliary class.
     */
    public static String toRecordInternalName(String recordName, String programClassName) {
        return programClassName + "$Record_" + recordName;
    }

    /**
     * Converts parameter types and return type into a method descriptor.
     * E.g. (I, D) -> I becomes "(ID)I".
     */
    public static String toMethodDescriptor(List<Type> parameterTypes, Type returnType, String programClassName) {
        StringBuilder sb = new StringBuilder("(");
        if (parameterTypes != null) {
            for (Type paramType : parameterTypes) {
                sb.append(toDescriptor(paramType, programClassName));
            }
        }
        sb.append(")");
        sb.append(toDescriptor(returnType, programClassName));
        return sb.toString();
    }
}
