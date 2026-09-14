package ru.innopolis;

import ru.innopolis.lexer.Scanner;
import ru.innopolis.lexer.Token;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        String filePath = "examples/1.txt";

        try {
            Path inputPath = Path.of(filePath);
            String sourceCode = Files.readString(inputPath);

            System.out.println("Analyzing file: " + filePath);

            Scanner scanner = new Scanner(sourceCode);
            List<Token> tokens = scanner.scanTokens();

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
            }

            System.out.println("Готово! Токены успешно сохранены в файл: " + outputPath);

        } catch (IOException e) {
            System.err.println("Ошибка ввода/вывода: " + e.getMessage());
        }
    }
}