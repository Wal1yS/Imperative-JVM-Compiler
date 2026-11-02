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
    private static int failures = 0;

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

        System.out.println("\n--- 3. Testing Pass 1 Scope Fixes ---");
        testExpectSuccess("Two records with the same field name",
                "type P is record var x: integer end\n type V is record var x: real end\n routine main() is end");
        testExpectError("Record field does not leak into enclosing scope",
                "type P is record var x: integer end\n routine main() is print x end",
                "Undeclared identifier 'x'");
        testExpectSuccess("Global variable named like a record field",
                "type P is record var x: integer end\n var x is 5\n routine main() is print x end");
        testExpectError("Duplicate record field",
                "type P is record var x: integer\n var x: integer end\n routine main() is end",
                "already declared");
        testExpectSuccess("Local type declaration",
                "routine main() is type T is integer\n var a: T is 1\n print a end");

        runTypeCheckerTests();
        runArrayBoundsTests();

        int totalFailures = failed + failures;
        System.out.println("\n==================================================");
        if (totalFailures == 0) {
            System.out.println("   ALL TESTS COMPLETED SUCCESSFULLY!              ");
        } else {
            System.out.println("   " + totalFailures + " TEST(S) FAILED");
        }
        System.out.println("==================================================");
    }

    private static final String MAIN = "\nroutine main() is end";
    private static final String POINT = "type P is record var x: integer end\n";

    private static void runTypeCheckerTests() {
        System.out.println("\n--- 4. Testing Person 2 Type Checks ---");

        testExpectSuccess("integer converted to real with 'as'",
                "routine main() is var r: real is 5 as real\n r := r + (1 as real)\n print r / 2.0 end");
        testExpectSuccess("for over array takes element type",
                "routine main() is var a: array [3] integer\n for v in a loop var w: integer is v\n print w end end");
        testExpectSuccess("Forward declaration with matching signature",
                "routine f(x: integer): integer\n routine main() is print f(1) end\n routine f(x: integer): integer is return x end");
        testExpectSuccess("Function call as a statement discards the result",
                "routine f(): integer is return 1 end\n routine main() is f() end");
        testExpectSuccess("Return on both if/else branches",
                "routine f(x: integer): integer is if x > 0 then return 1 else return 0 end end" + MAIN);
        testExpectSuccess("Valid casts",
                "routine main() is var a is true as real\n var b is 1 as boolean\n var c is 2.7 as integer\n print a, b, c end");

        testExpectError("boolean initializer for integer",
                "routine main() is var r: integer is true end", "Cannot initialize variable 'r' of type integer");
        testExpectError("integer initializer for boolean",
                "routine main() is var b: boolean is 1 end", "Cannot initialize variable 'b' of type boolean");
        testExpectError("real assigned to integer",
                "routine main() is var i: integer is 1\n i := 2.5 end", "cannot assign real to integer");
        testExpectError("integer initializer for real",
                "routine main() is var r: real is 5 end", "Cannot initialize variable 'r' of type real with a value of type integer");
        testExpectError("integer assigned to real",
                "routine main() is var r: real is 1.0\n r := 1 end", "cannot assign integer to real");
        testExpectError("real plus integer",
                "routine main() is print 1.5 + 1 end", "Operator '+' cannot be applied to real and integer");
        testExpectError("integer compared with real",
                "routine main() is print 1 < 2.5 end", "Operator '<' cannot be applied to integer and real");
        testExpectError("integer equals real",
                "routine main() is print 1 = 1.0 end", "Operator '=' cannot be applied to integer and real");
        testExpectError("integer argument for real parameter",
                "routine f(x: real): real is return x end\n routine main() is print f(1) end", "cannot pass integer as real");
        testExpectError("integer returned from real function",
                "routine f(): real is return 1 end" + MAIN, "expected real, got integer");
        testExpectError("boolean in arithmetic",
                "routine main() is print true + 1 end", "Operator '+' cannot be applied to boolean and integer");
        testExpectError("% on real",
                "routine main() is print 5.0 % 2 end", "Operator '%' cannot be applied to real and integer");
        testExpectError("Records are compared by name",
                "type A is record var x: integer end\n type B is record var x: integer end\n"
                        + "routine main() is var a: A\n var b: B\n a := b end", "cannot assign B to A");

        testExpectError("string plus integer",
                "routine main() is print 1 + \"a\" end", "Operator '+' cannot be applied to integer and string");
        testExpectError("string multiplication",
                "routine main() is var s: string is \"a\"\n print s * s end", "Operator '*' cannot be applied to string and string");

        testExpectError("real as boolean",
                "routine main() is var x is 3.5 as boolean end", "Cannot convert real to boolean");
        testExpectError("record as integer",
                POINT + "routine main() is var p: P\n var n is p as integer end", "Cannot convert P to integer");
        testExpectError("cast to a non-primitive type",
                "routine main() is var b is 1 as array integer end", "'as' converts only to primitive types");

        testExpectError("unknown array member",
                "routine main() is var a: array [3] integer\n print a.foo end", "Arrays have no member 'foo'");
        testExpectError("assignment to .size",
                "routine main() is var a: array [3] integer\n a.size := 5 end", "read-only property 'size'");
        testExpectError("unknown record field",
                POINT + "routine main() is var p: P\n print p.y end", "has no field 'y'");
        testExpectError("member of a primitive",
                "routine main() is var n is 1\n print n.x end", "Cannot access member 'x' of a value of type integer");

        testExpectError("real index",
                "routine main() is var a: array [3] integer\n print a[1.5] end", "Array index must be integer, got real");
        testExpectError("indexing a non-array",
                "routine main() is var n is 1\n print n[0] end", "it is not an array");
        testExpectError("sizeless array variable",
                "routine main() is var a: array integer end", "needs an array size");
        testExpectError("non-positive array size",
                "routine main() is var a: array [0] integer end", "Array size must be positive");
        testExpectError("non-integer array size",
                "routine main() is var a: array [2.5] integer end", "Array size must be integer");

        testExpectError("value returned from a procedure",
                "routine p() is return 1 end" + MAIN, "has no return type and cannot return a value");
        testExpectError("empty return in a function",
                "routine f(): integer is return\n end" + MAIN, "must return a value of type integer");
        testExpectError("return type mismatch",
                "routine f(): integer is return true end" + MAIN, "expected integer, got boolean");
        testExpectError("missing return on some path",
                "routine f(x: integer): integer is if x > 0 then return 1 end end" + MAIN, "on every path");
        testExpectError("expression body type mismatch",
                "routine f(x: integer): boolean => x + 1" + MAIN, "expression body has type integer");

        testExpectError("wrong argument count",
                "routine f(a: integer, b: integer): integer is return a + b end\n routine main() is print f(1) end",
                "expects 2 argument(s), but 1 were given");
        testExpectError("wrong argument type",
                "routine f(a: integer, b: integer): integer is return a + b end\n routine main() is print f(1, true) end",
                "Argument 2 of routine 'f': cannot pass boolean as integer");
        testExpectError("procedure call used as a value",
                "routine p() is print 1 end\n routine main() is var x is p() end", "cannot be used as a value");
        testExpectError("routine used as a variable",
                "routine f(): integer is return 1 end\n routine main() is print f end", "'f' is a routine, not a variable");

        testExpectError("integer if condition",
                "routine main() is if 1 then print 1 end end", "Condition of 'if' must be boolean, got integer");
        testExpectError("integer while condition",
                "routine main() is while 0 loop print 1 end end", "Condition of 'while' must be boolean");
        testExpectError("real range bound",
                "routine main() is for i in 0 .. 2.5 loop print i end end", "Range bounds of 'for' must be integer, got real");
        testExpectError("for-in over a non-array",
                "routine main() is for v in 5 loop print v end end", "requires an array, got integer");
        testExpectError("print a record",
                POINT + "routine main() is var p: P\n print p end", "Cannot print a value of type P");

        testExpectError("forward declaration signature mismatch",
                "routine f(x: integer): integer\n routine f(x: real): integer is return 1 end" + MAIN,
                "forward declaration says integer");
        testExpectError("recursive record type",
                "type N is record var next: N end" + MAIN, "Recursive type definition 'N'");
    }

    private static void runArrayBoundsTests() {
        System.out.println("\n--- 5. Testing Person 2 Array Bounds Checks ---");
        String arr = "routine main() is var a: array [10] integer\n";

        testExpectSuccess("in-bounds loop over 0 .. 9", arr + "for i in 0 .. 9 loop a[i] := i end end");
        testExpectSuccess("index guarded by if", arr + "for i in 0 .. 9 loop if i > 0 then a[i - 1] := i end end end");
        testExpectSuccess("run-time index is not checked", arr + "var k is 20\n a[k] := 1 end");

        testExpectError("constant index equal to size", arr + "a[10] := 1 end", "index is 10, array size is 10");
        testExpectError("negative constant index", arr + "a[-1] := 1 end", "index is -1");
        testExpectError("constant expression index",
                "type Arr is array [5] integer\n routine main() is var a: Arr\n print a[2 + 3] end", "index is 5, array size is 5");
        testExpectError("1-based loop on a 0-based array", arr + "for i in 1 .. 10 loop a[i] := i end end",
                "index takes values 1..10, array size is 10");
        testExpectError("loop variable plus offset", arr + "for i in 0 .. 9 loop print a[i + 1] end end",
                "index takes values 1..10");
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
            failures++;
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
                    failures++;
                    for (SemanticError e : result.getErrors()) System.err.println("       Actual: " + e);
                }
            } else {
                System.err.println("[FAIL] " + testName + " unexpectedly succeeded with no errors!");
                failures++;
            }
        } catch (Exception e) {
            System.err.println("[FAIL] " + testName + " threw parser/scanner exception: " + e.getMessage());
            failures++;
        }
    }

    private static void testExpectSuccess(String testName, String code) {
        try {
            AnalysisResult result = runAnalysis(code);
            if (result.isSuccess()) {
                System.out.println("[PASS] " + testName + " succeeded as expected.");
            } else {
                System.err.println("[FAIL] " + testName + " failed with errors:");
                failures++;
                for (SemanticError e : result.getErrors()) System.err.println("       " + e);
            }
        } catch (Exception e) {
            System.err.println("[FAIL] " + testName + " threw exception: " + e.getMessage());
            failures++;
        }
    }
}
