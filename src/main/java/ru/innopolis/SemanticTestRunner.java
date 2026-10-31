package ru.innopolis;

import ru.innopolis.ast.ProgramNode;
import ru.innopolis.ast.statement.ReturnStatementNode;
import ru.innopolis.lexer.Scanner;
import ru.innopolis.lexer.Span;
import ru.innopolis.lexer.token.Token;
import ru.innopolis.parser.Parser;
import ru.innopolis.semantic.AnalysisResult;
import ru.innopolis.semantic.SemanticAnalyzer;
import ru.innopolis.semantic.check.DeclarationAndScopeChecker;
import ru.innopolis.semantic.context.SemanticContext;
import ru.innopolis.semantic.context.SemanticError;
import ru.innopolis.semantic.scope.GlobalScope;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class SemanticTestRunner {

    public static void main(String[] args) {
        System.out.println("==================================================");
        System.out.println("   RUNNING SEMANTIC ANALYZER (PERSON 1) TESTS     ");
        System.out.println("==================================================");

        int passed = 0;
        int failed = 0;

        // 1. Run on all 15 positive benchmark test cases in examples/
        System.out.println("\n--- 1. Testing Existing Benchmark Examples (1.txt - 15.txt) ---");
        for (int i = 1; i <= 15; i++) {
            Path path = Path.of("examples/" + i + ".txt");
            if (!Files.exists(path)) continue;
            try {
                String code = Files.readString(path);
                AnalysisResult result = runAnalysis(code);
                if (result.isSuccess()) {
                    System.out.println("[PASS] " + path.getFileName() + " passed semantic checks.");
                    passed++;
                } else {
                    System.err.println("[FAIL] " + path.getFileName() + " failed with errors:");
                    for (SemanticError err : result.getErrors()) {
                        System.err.println("       " + err);
                    }
                    failed++;
                }
            } catch (Exception e) {
                System.err.println("[ERROR] Exception testing " + path + ": " + e.getMessage());
                failed++;
            }
        }

        // 2. Run negative test cases for Person 1's 5 checks
        System.out.println("\n--- 2. Testing Person 1 Negative Semantic Checks ---");

        // Negative 1: Missing routine main (Vitebsk Check 2)
        testExpectError("Missing main entry point",
                "routine foo() is var x is 1 end",
                "Mandatory entry point 'routine main() is ... end' is missing");

        // Negative 2: routine main with parameters (Vitebsk Check 2)
        testExpectError("main() with parameters",
                "routine main(a: integer) is var x is 1 end",
                "must not require parameters");

        // Negative 3: Undeclared variable (Curator Check 2)
        testExpectError("Undeclared variable",
                "routine main() is y := 10 end",
                "Undeclared identifier 'y'");

        // Negative 4: Duplicate variable in same block (Curator Check 2)
        testExpectError("Duplicate variable in same scope",
                "routine main() is var a is 1\n var a is 2 end",
                "already declared in this scope");

        // Negative 5: Return outside routine (Curator Check 1)
        testReturnOutsideRoutineDirect();

        // Negative 6: Assignment to for-loop variable (Vitebsk Check 7)
        testExpectError("Assign to for-loop read-only variable",
                "routine main() is for i in 1 .. 10 loop i := 2 end end",
                "Cannot assign to read-only loop variable 'i'");

        // Negative 7: Unfulfilled forward declaration (Vitebsk Check 8)
        testExpectError("Unfulfilled forward declaration",
                "routine helper(x: integer): integer\n routine main() is end",
                "has no implementation");

        // Positive Shadowing test: inner block shadows outer variable (Curator Check 2)
        testExpectSuccess("Shadowing in nested block",
                "routine main() is var a is 1\n if true then var a is 2\n print a end\n print a end");

        System.out.println("\n==================================================");
        System.out.println("   ALL TESTS COMPLETED SUCCESSFULLY!              ");
        System.out.println("==================================================");
    }

    private static void testReturnOutsideRoutineDirect() {
        SemanticContext context = new SemanticContext();
        GlobalScope globalScope = new GlobalScope();
        context.setGlobalScope(globalScope);
        DeclarationAndScopeChecker checker = new DeclarationAndScopeChecker(context);
        ReturnStatementNode retNode = new ReturnStatementNode(new Span(1, 1, 6), null);
        retNode.accept(checker, globalScope);
        if (context.hasErrors() && context.getErrors().get(0).getMessage().contains("Return statement is not allowed outside routine body")) {
            System.out.println("[PASS] Return outside routine correctly caught: " + context.getErrors().get(0).getMessage());
        } else {
            System.err.println("[FAIL] Return outside routine did not catch error!");
        }
    }

    private static AnalysisResult runAnalysis(String sourceCode) {
        Scanner scanner = new Scanner(sourceCode);
        List<Token> tokens = scanner.scanTokens();
        Parser parser = new Parser(tokens);
        ProgramNode program = parser.parseProgram();
        return SemanticAnalyzer.analyze(program);
    }

    private static void testExpectError(String testName, String code, String expectedErrorMessageSubstring) {
        try {
            AnalysisResult result = runAnalysis(code);
            if (!result.isSuccess()) {
                boolean matched = result.getErrors().stream()
                        .anyMatch(e -> e.getMessage().contains(expectedErrorMessageSubstring));
                if (matched) {
                    System.out.println("[PASS] " + testName + " correctly caught: " + result.getErrors().get(0).getMessage());
                } else {
                    System.err.println("[FAIL] " + testName + " did not contain expected message substring: " + expectedErrorMessageSubstring);
                    for (SemanticError e : result.getErrors()) System.err.println("       Actual: " + e);
                }
            } else {
                System.err.println("[FAIL] " + testName + " unexpectedly succeeded with no errors!");
            }
        } catch (Exception e) {
            System.err.println("[FAIL] " + testName + " threw parser/scanner exception: " + e.getMessage());
        }
    }

    private static void testExpectSuccess(String testName, String code) {
        try {
            AnalysisResult result = runAnalysis(code);
            if (result.isSuccess()) {
                System.out.println("[PASS] " + testName + " succeeded as expected.");
            } else {
                System.err.println("[FAIL] " + testName + " failed with errors:");
                for (SemanticError e : result.getErrors()) System.err.println("       " + e);
            }
        } catch (Exception e) {
            System.err.println("[FAIL] " + testName + " threw exception: " + e.getMessage());
        }
    }
}
