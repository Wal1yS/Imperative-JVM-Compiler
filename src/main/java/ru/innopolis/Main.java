package ru.innopolis;

import ru.innopolis.ast.ASTPrinter;
import ru.innopolis.ast.ProgramNode;
import ru.innopolis.lexer.Scanner;
import ru.innopolis.lexer.token.Token;
import ru.innopolis.parser.Parser;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        String filePath = "examples/2.txt";

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

            // 3. Write outputs
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
            }

            System.out.println("Готово! Токены и AST успешно сохранены в файл: " + outputPath);

        } catch (IOException e) {
            System.err.println("Ошибка ввода/вывода: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Ошибка синтаксического анализа: " + e.getMessage());
        }
    }
}