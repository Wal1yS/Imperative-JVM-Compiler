package ru.innopolis;

import ru.innopolis.ast.ASTPrinter;
import ru.innopolis.ast.ProgramNode;
import ru.innopolis.codegen.BytecodePrinter;
import ru.innopolis.codegen.JVMCodeGenerator;
import ru.innopolis.codegen.runtime.BytecodeClassLoader;
import ru.innopolis.lexer.Scanner;
import ru.innopolis.lexer.token.Token;
import ru.innopolis.parser.Parser;
import ru.innopolis.semantic.AnalysisResult;
import ru.innopolis.semantic.SemanticAnalyzer;
import ru.innopolis.semantic.context.SemanticError;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public class Main {
    public static void main(String[] args) {
        String filePath = (args != null && args.length > 0) ? args[0] : "examples/error_accumulation_pass1.txt";

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
                return;
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

            // 6. JVM Bytecode Generation & Execution (Person 1)
            try {
                System.out.println("\n--- JVM BYTECODE GENERATION ---");
                String baseName = inputPath.getFileName().toString().replaceAll("\\.[^.]+$", "");
                String className = "Compiled_" + baseName;
                Map<String, byte[]> generatedClasses = JVMCodeGenerator.compile(result, className);

                for (Map.Entry<String, byte[]> entry : generatedClasses.entrySet()) {
                    String clsName = entry.getKey();
                    byte[] bytecode = entry.getValue();

                    // 1. Save binary .class file
                    Path classFile = outputDir.resolve(clsName + ".class");
                    Files.write(classFile, bytecode);
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

                System.out.println("\n--- JVM DIRECT EXECUTION OUTPUT ---");
                BytecodeClassLoader loader = new BytecodeClassLoader();
                loader.addClasses(generatedClasses);
                Class<?> compiledClass = loader.loadClass(className);
                Method mainMethod = compiledClass.getMethod("main", String[].class);
                mainMethod.invoke(null, (Object) new String[0]);
                System.out.println("--- JVM EXECUTION COMPLETED ---");
            } catch (UnsupportedOperationException e) {
                System.out.println("[NOTE] Codegen extension hook: " + e.getMessage());
            } catch (InvocationTargetException e) {
                Throwable cause = e.getCause() != null ? e.getCause() : e;
                System.err.println("Runtime error in compiled program: " + cause);
            } catch (Exception e) {
                System.err.println("Codegen error: " + e);
            }

        } catch (IOException e) {
            System.err.println("IO Error: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Analysis Error: " + e.getMessage());
        }
    }
}
