package ru.innopolis.codegen;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.util.TraceClassVisitor;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * Disassembles JVM bytecode into clean, human-readable text format.
 * Uses OW2 ASM TraceClassVisitor to display pure JVM instructions and class structures.
 */
public final class BytecodePrinter {

    private BytecodePrinter() {}

    /**
     * Converts raw compiled .class byte array into a readable JVM bytecode listing.
     */
    public static String disassemble(byte[] bytecode) {
        if (bytecode == null || bytecode.length == 0) {
            return "<empty bytecode>";
        }
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        ClassReader cr = new ClassReader(bytecode);
        TraceClassVisitor tcv = new TraceClassVisitor(pw);
        cr.accept(tcv, 0);
        return sw.toString().trim();
    }
}
