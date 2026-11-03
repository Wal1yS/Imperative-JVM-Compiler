package ru.innopolis;

import ru.innopolis.ast.ASTPrinter;
import ru.innopolis.ast.ProgramNode;
import ru.innopolis.lexer.Scanner;
import ru.innopolis.lexer.token.Token;
import ru.innopolis.parser.Parser;
import ru.innopolis.semantic.AnalysisResult;
import ru.innopolis.semantic.SemanticAnalyzer;
import ru.innopolis.semantic.context.SemanticError;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        String filePath = (args != null && args.length > 0) ? args[0] : "examples/13.txt";

        try {
            Path inputPath = Path.of(filePath);
            if (!Files.exists(inputPath)) {
                System.err.println("File not found: " + filePath);
                return;
            }

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

            // Ensure output directory exists
            Path outputDir = Path.of("output");
            if (!Files.exists(outputDir)) {
                Files.createDirectories(outputDir);
            }
            String fileName = inputPath.getFileName().toString();
            Path outputPath = outputDir.resolve(fileName);

            // 3. Semantic Analysis (Passes 1 - 4)
            System.out.println("--- SEMANTIC ANALYSIS ---");
            AnalysisResult result = SemanticAnalyzer.analyze(program);

            if (!result.isSuccess()) {
                System.err.println("FAILED: Found " + result.getErrors().size() + " semantic error(s):");
                for (SemanticError error : result.getErrors()) {
                    System.err.println("  " + error);
                }

                // Write failure details to output file
                try (PrintWriter writer = new PrintWriter(Files.newBufferedWriter(outputPath))) {
                    writer.println("--- SOURCE CODE ---");
                    writer.println(sourceCode);
                    writer.println("--- TOKENS ---");
                    for (Token token : tokens) {
                        writer.println(token);
                    }
                    writer.println("\n--- ORIGINAL AST ---");
                    writer.println(originalAstString);
                    writer.println("\n--- SEMANTIC STATUS ---");
                    writer.println("FAILED: Semantic errors detected (Optimizations skipped):");
                    for (SemanticError error : result.getErrors()) {
                        writer.println("  " + error);
                    }
                }
                System.out.println("\nAnalysis failed. Error details saved to: " + outputPath);
                return;
            }

            // If successful:
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

            // 5. Write successful outputs
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

        } catch (IOException e) {
            System.err.println("IO Error: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Analysis Error: " + e.getMessage());
        }
    }
}
