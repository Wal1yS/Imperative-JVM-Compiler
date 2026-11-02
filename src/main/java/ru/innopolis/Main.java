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
            String sourceCode = Files.readString(inputPath);

            System.out.println("Analyzing file: " + filePath);

            // 1. Lexical Analysis
            Scanner scanner = new Scanner(sourceCode);
            List<Token> tokens = scanner.scanTokens();

            // 2. Syntax Analysis
            Parser parser = new Parser(tokens);
            ProgramNode program = parser.parseProgram();

            String astString = ASTPrinter.print(program);
            System.out.println("--- AST TREE ---");
            System.out.println(astString);

            // 3. Semantic Analysis (scopes, types, array bounds)
            System.out.println("--- SEMANTIC ANALYSIS ---");
            AnalysisResult result = SemanticAnalyzer.analyze(program);
            if (!result.isSuccess()) {
                System.err.println("Found " + result.getErrors().size() + " semantic error(s):");
                for (SemanticError error : result.getErrors()) {
                    System.err.println("  " + error);
                }
                return;
            }
            System.out.println("Semantic checks passed successfully!");

            // 4. Write outputs
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
                writer.println("\n--- AST ---");
                writer.println(astString);
                writer.println("\n--- SEMANTIC STATUS ---");
                writer.println("Pass 1: Scope & Declarations OK");
                writer.println("Pass 2: Types OK");
                writer.println("Pass 3: Array Bounds OK");
            }

            System.out.println("Готово! Токены и AST успешно сохранены в файл: " + outputPath);

        } catch (IOException e) {
            System.err.println("Ошибка ввода/вывода: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Ошибка анализа: " + e.getMessage());
        }
    }
}
