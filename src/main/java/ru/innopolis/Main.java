package ru.innopolis;

import ru.innopolis.ast.ASTPrinter;
import ru.innopolis.ast.ProgramNode;
import ru.innopolis.codegen.BytecodePrinter;
import ru.innopolis.codegen.JVMCodeGenerator;
import ru.innopolis.runtime.BytecodeClassLoader;
import ru.innopolis.runtime.ClassFileWriter;
import ru.innopolis.lexer.Scanner;
import ru.innopolis.lexer.token.Token;
import ru.innopolis.parser.Parser;
import ru.innopolis.semantic.AnalysisResult;
import ru.innopolis.semantic.SemanticAnalyzer;
import ru.innopolis.semantic.context.SemanticError;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public class Main {
    public static void main(String[] args) {
        int status = run(args);
        if (status != 0) System.exit(status);
    }

    private static int run(String[] args) {
        boolean compileOnly = args.length > 0 && "--compile".equals(args[0]);
        boolean runFlag = args.length > 0 && "--run".equals(args[0]);
        int input = compileOnly || runFlag ? 1 : 0;
        if (args.length != 0 && (args.length != input + 1 || args[input].startsWith("--"))) {
            System.err.println("Usage: Main [--compile | --run] <source-file>");
            return 2;
        }
        String filePath = args.length == 0 ? "examples/1.txt" : args[input];

        try {
            Path inputPath = Path.of(filePath);
            String sourceCode = Files.readString(inputPath);

            System.out.println("Analyzing file: " + filePath);

            // 1. Lexical Analysis
            Scanner scanner = new Scanner(sourceCode);
            List<Token> tokens = scanner.scanTokens();

            // 2. Syntax Analysis
            Parser parser = new Parser(tokens);
            ProgramNode program = parser.parseProgram();

            String originalAstString = ASTPrinter.print(program);
            System.out.println("--- ORIGINAL AST TREE ---");
            System.out.println(originalAstString);

            // 3. Semantic Analysis (Passes 1 - 4)
            System.out.println("--- SEMANTIC ANALYSIS ---");
            AnalysisResult result = SemanticAnalyzer.analyze(program);
            if (!result.isSuccess()) {
                System.err.println("Found " + result.getErrors().size() + " semantic error(s):");
                for (SemanticError error : result.getErrors()) {
                    System.err.println("  " + error);
                }
                return 1;
            }

            System.out.println("Pass 1: Scope & Declarations OK");
            System.out.println("Pass 2: Types OK");
            System.out.println("Pass 3: Array Bounds OK");
            System.out.println("Pass 4: AST Optimizations OK");
            System.out.println("Semantic checks and optimizations passed successfully!");

            // 4. Output Optimized AST Tree
            String optimizedAstString = ASTPrinter.print(result.getProgram());
            boolean wasModified = !optimizedAstString.equals(originalAstString);

            System.out.println("\n--- OPTIMIZED AST TREE (Pass 4) ---");
            if (wasModified) {
                System.out.println("(AST was transformed: constant folding / dead code / unused variables applied)");
            } else {
                System.out.println("(No optimizations applicable for this program)");
            }
            System.out.println(optimizedAstString);

            // 5. Write outputs
            Path outputDir = Path.of("output");
            if (!Files.exists(outputDir)) {
                Files.createDirectories(outputDir);
            }

            String fileName = inputPath.getFileName().toString();
            Path outputPath = outputDir.resolve(fileName);

            try (PrintWriter writer = new PrintWriter(Files.newBufferedWriter(outputPath))) {
                writer.println("--- SOURCE CODE ---");
                writer.println(sourceCode);
                writer.println("--- TOKENS ---");
                for (Token token : tokens) {
                    writer.println(token);
                }
                writer.println("\n--- ORIGINAL AST ---");
                writer.println(originalAstString);
                writer.println("\n--- OPTIMIZED AST ---");
                writer.println(optimizedAstString);
                writer.println("\n--- SEMANTIC STATUS ---");
                writer.println("Pass 1: Scope & Declarations OK");
                writer.println("Pass 2: Types OK");
                writer.println("Pass 3: Array Bounds OK");
                writer.println("Pass 4: AST Optimizations OK");
            }

            System.out.println("\nDone! Analysis results and optimized AST saved to: " + outputPath);

            // 6. JVM bytecode generation, export and optional execution
            System.out.println("\n--- JVM BYTECODE GENERATION ---");
            String className = "CompiledProgram";
            Map<String, byte[]> generatedClasses = JVMCodeGenerator.compile(result, className);
            ClassFileWriter.writeAll(outputDir, generatedClasses);

            for (Map.Entry<String, byte[]> entry : generatedClasses.entrySet()) {
                String clsName = entry.getKey();
                byte[] bytecode = entry.getValue();

                // 1. Save binary .class file
                Path classFile = outputDir.resolve(clsName + ".class");
                System.out.println("Generated JVM binary: " + classFile);

                // 2. Disassemble into pure human-readable JVM bytecode
                String disassembly = BytecodePrinter.disassemble(bytecode);

                // Save .jvm disassembly text file
                Path jvmTextFile = outputDir.resolve(clsName + ".jvm");
                Files.writeString(jvmTextFile, disassembly);
                System.out.println("Saved pure JVM disassembly: " + jvmTextFile);

                // Print pure JVM bytecode directly to console
                System.out.println("\n--- PURE JVM BYTECODE LISTING (" + clsName + ") ---");
                System.out.println(disassembly);
            }

            if (compileOnly) return 0;
            System.out.println("\n--- JVM DIRECT EXECUTION OUTPUT ---");
            BytecodeClassLoader loader = new BytecodeClassLoader();
            loader.addClasses(generatedClasses);
            loader.runMain(className);
            System.out.println("--- JVM EXECUTION COMPLETED ---");
            return 0;
        } catch (InvocationTargetException | ExceptionInInitializerError e) {
            System.err.println("Runtime error in compiled program: " + e.getCause());
        } catch (IOException e) {
            System.err.println("IO Error: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Analysis Error: " + e.getMessage());
        }
        return 1;
    }
}
