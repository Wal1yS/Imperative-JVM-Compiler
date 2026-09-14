package ru.innopolis;

import ru.innopolis.lexer.Scanner;
import ru.innopolis.lexer.Token;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        String filePath = "examples/1.imp";

        try {
            String sourceCode = Files.readString(Path.of(filePath));

            System.out.println("Analyzing file: " + filePath);
            System.out.println("--- SOURCE CODE ---");
            System.out.println(sourceCode);
            System.out.println("--- TOKENS ---");

            Scanner scanner = new Scanner(sourceCode);
            List<Token> tokens = scanner.scanTokens();

            for (Token token : tokens) {
                System.out.println(token.toString());
            }

        } catch (IOException e) {
            System.err.println("Can't read file: " + e.getMessage());
        }
    }
}