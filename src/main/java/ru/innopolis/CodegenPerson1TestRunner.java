package ru.innopolis;

import ru.innopolis.ast.ProgramNode;
import ru.innopolis.codegen.JVMCodeGenerator;
import ru.innopolis.codegen.context.LocalVariableIndexManager;
import ru.innopolis.codegen.runtime.BytecodeClassLoader;
import ru.innopolis.lexer.Scanner;
import ru.innopolis.lexer.token.Token;
import ru.innopolis.parser.Parser;
import ru.innopolis.semantic.AnalysisResult;
import ru.innopolis.semantic.SemanticAnalyzer;
import ru.innopolis.semantic.symbol.Symbol;
import ru.innopolis.semantic.symbol.VariableSymbol;
import ru.innopolis.semantic.type.PrimitiveType;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

/**
 * Verification test runner for Person 1 (Valerii Tiniakov):
 * Tests JVM bytecode generation, method signatures, local slot allocation,
 * arithmetic, casts, global variables, routine calls, and standard main() entry point.
 */
public class CodegenPerson1TestRunner {

    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) {
        System.out.println("==================================================");
        System.out.println("   RUNNING CODEGEN PERSON 1 TESTS (VALERII)       ");
        System.out.println("==================================================");

        testIntegerArithmetic();
        testRealArithmetic();
        testExplicitCasts();
        testGlobalVariables();
        testRoutineCallsWithMultipleParameters();
        testRecursiveFibonacci();
        testExpressionBodyRoutine();
        testLogicalAndComparisonOperators();
        testJVMMainEntryPointExecution();
        testRealComparisonsWithNaN();
        testStringDefaultValue();
        testScopedSlotReuse();
        testBlockLocalsExecution();

        System.out.println("\n==================================================");
        System.out.println("RESULTS: " + passed + " PASSED, " + failed + " FAILED");
        System.out.println("==================================================");

        if (failed > 0) {
            System.exit(1);
        }
    }

    private static Class<?> compileToClass(String source, String className) throws Exception {
        Scanner scanner = new Scanner(source);
        List<Token> tokens = scanner.scanTokens();
        Parser parser = new Parser(tokens);
        ProgramNode program = parser.parseProgram();

        AnalysisResult analysisResult = SemanticAnalyzer.analyze(program);
        if (!analysisResult.isSuccess()) {
            throw new IllegalStateException("Semantic analysis failed: " + analysisResult.getErrors());
        }

        Map<String, byte[]> classes = JVMCodeGenerator.compile(analysisResult, className);
        BytecodeClassLoader loader = new BytecodeClassLoader();
        loader.addClasses(classes);
        return loader.loadClass(className);
    }

    private static void check(String testName, boolean condition, String details) {
        if (condition) {
            System.out.println("[PASS] " + testName);
            passed++;
        } else {
            System.err.println("[FAIL] " + testName + ": " + details);
            failed++;
        }
    }

    private static void testIntegerArithmetic() {
        try {
            String code = """
                routine compute(): integer is
                    return (10 + 20) * 3 - 50 / 2 % 7
                end
                routine main() is
                    compute()
                end
                """;
            Class<?> clazz = compileToClass(code, "TestIntArith");
            Method method = clazz.getMethod("compute");
            int result = (int) method.invoke(null);
            // (10+20)*3 = 90; 50/2 = 25; 25 % 7 = 4; 90 - 4 = 86
            check("Integer arithmetic and operator precedence", result == 86, "Expected 86, got " + result);
        } catch (Exception e) {
            check("Integer arithmetic and operator precedence", false, e.getMessage());
        }
    }

    private static void testRealArithmetic() {
        try {
            String code = """
                routine calc(a: real, b: real): real is
                    return (a + b) * 2.0 / 2.5
                end
                routine main() is
                    calc(3.5, 1.5)
                end
                """;
            Class<?> clazz = compileToClass(code, "TestRealArith");
            Method method = clazz.getMethod("calc", double.class, double.class);
            double result = (double) method.invoke(null, 3.5, 1.5);
            // (3.5 + 1.5) = 5.0 * 2.0 = 10.0 / 2.5 = 4.0
            check("Real arithmetic (double slots)", Math.abs(result - 4.0) < 1e-6, "Expected 4.0, got " + result);
        } catch (Exception e) {
            check("Real arithmetic (double slots)", false, e.getMessage());
        }
    }

    private static void testExplicitCasts() {
        try {
            String code = """
                routine castIntToReal(x: integer): real is
                    return x as real
                end
                routine castRealToInt(r: real): integer is
                    return r as integer
                end
                routine castBoolToInt(b: boolean): integer is
                    return b as integer
                end
                routine castIntToBool(i: integer): boolean is
                    return i as boolean
                end
                routine main() is
                end
                """;
            Class<?> clazz = compileToClass(code, "TestCasts");

            double rVal = (double) clazz.getMethod("castIntToReal", int.class).invoke(null, 42);
            int iVal = (int) clazz.getMethod("castRealToInt", double.class).invoke(null, 7.6);
            int bValTrue = (int) clazz.getMethod("castBoolToInt", boolean.class).invoke(null, true);
            int bValFalse = (int) clazz.getMethod("castBoolToInt", boolean.class).invoke(null, false);
            boolean boolVal = (boolean) clazz.getMethod("castIntToBool", int.class).invoke(null, 1);

            check("Explicit cast integer as real", rVal == 42.0, "Expected 42.0, got " + rVal);
            check("Explicit cast real as integer with rounding", iVal == 8, "Expected 8 (7.6 rounded), got " + iVal);
            check("Explicit cast boolean as integer (true -> 1, false -> 0)", bValTrue == 1 && bValFalse == 0,
                    "Expected 1 and 0, got " + bValTrue + ", " + bValFalse);
            check("Explicit cast integer as boolean", boolVal, "Expected true, got false");
        } catch (Exception e) {
            check("Explicit casts", false, e.getMessage());
        }
    }

    private static void testGlobalVariables() {
        try {
            String code = """
                var globalCounter: integer is 100
                routine getGlobal(): integer is
                    return globalCounter
                end
                routine main() is
                    getGlobal()
                end
                """;
            Class<?> clazz = compileToClass(code, "TestGlobals");
            int val = (int) clazz.getMethod("getGlobal").invoke(null);
            check("Static <clinit> global variable initialization and reading", val == 100, "Expected 100, got " + val);
        } catch (Exception e) {
            check("Static <clinit> global variable initialization and reading", false, e.getMessage());
        }
    }

    private static void testRoutineCallsWithMultipleParameters() {
        try {
            String code = """
                routine sumFour(a: integer, b: integer, c: integer, d: integer): integer is
                    return a + b + c + d
                end
                routine main() is
                    sumFour(1, 2, 3, 4)
                end
                """;
            Class<?> clazz = compileToClass(code, "TestParams");
            int res = (int) clazz.getMethod("sumFour", int.class, int.class, int.class, int.class)
                    .invoke(null, 10, 20, 30, 40);
            check("Routine calls with 4 parameters", res == 100, "Expected 100, got " + res);
        } catch (Exception e) {
            check("Routine calls with 4 parameters", false, e.getMessage());
        }
    }

    private static void testRecursiveFibonacci() {
        try {
            String code = """
                routine fib(n: integer): integer is
                    if n <= 1 then
                        return n
                    end
                    return fib(n - 1) + fib(n - 2)
                end
                routine main() is
                    fib(7)
                end
                """;
            Class<?> clazz = compileToClass(code, "TestFib");
            int res7 = (int) clazz.getMethod("fib", int.class).invoke(null, 7);
            int res10 = (int) clazz.getMethod("fib", int.class).invoke(null, 10);
            check("Recursive routine calls with branches (Fibonacci)", res7 == 13 && res10 == 55,
                    "Expected fib(7)=13, fib(10)=55; got " + res7 + ", " + res10);
        } catch (Exception e) {
            check("Recursive routine calls with branches (Fibonacci)", false, e.getMessage());
        }
    }

    private static void testExpressionBodyRoutine() {
        try {
            String code = """
                routine square(x: integer): integer => x * x
                routine main() is
                    square(9)
                end
                """;
            Class<?> clazz = compileToClass(code, "TestExprBody");
            int res = (int) clazz.getMethod("square", int.class).invoke(null, 9);
            check("Expression body routine (=>)", res == 81, "Expected 81, got " + res);
        } catch (Exception e) {
            check("Expression body routine (=>)", false, e.getMessage());
        }
    }

    private static void testLogicalAndComparisonOperators() {
        try {
            String code = """
                routine checkRel(a: integer, b: integer): boolean is
                    return (a < b) and (b > 0) or (a = b)
                end
                routine main() is
                    checkRel(5, 10)
                end
                """;
            Class<?> clazz = compileToClass(code, "TestRel");
            boolean res1 = (boolean) clazz.getMethod("checkRel", int.class, int.class).invoke(null, 5, 10);
            boolean res2 = (boolean) clazz.getMethod("checkRel", int.class, int.class).invoke(null, 20, 10);
            boolean res3 = (boolean) clazz.getMethod("checkRel", int.class, int.class).invoke(null, 10, 10);
            check("Logical and relational operations (and, or, <, >, =)", res1 && !res2 && res3,
                    "Expected true, false, true; got " + res1 + ", " + res2 + ", " + res3);
        } catch (Exception e) {
            check("Logical and relational operations", false, e.getMessage());
        }
    }

    private static void testJVMMainEntryPointExecution() {
        try {
            String code = """
                var executed: boolean is false
                routine main() is
                    executed := true
                end
                """;
            Class<?> clazz = compileToClass(code, "TestMainEntry");
            Method jvmMain = clazz.getMethod("main", String[].class);
            jvmMain.invoke(null, (Object) new String[0]);

            boolean executed = clazz.getField("executed").getBoolean(null);
            check("Standard public static void main(String[] args) JVM bridge execution", executed,
                    "Expected executed=true, got " + executed);
        } catch (Exception e) {
            check("Standard public static void main(String[] args) JVM bridge execution", false, e.getMessage());
        }
    }

    private static void testRealComparisonsWithNaN() {
        try {
            String code = """
                routine gt(a: real, b: real): boolean => a > b
                routine ge(a: real, b: real): boolean => a >= b
                routine lt(a: real, b: real): boolean => a < b
                routine le(a: real, b: real): boolean => a <= b
                routine eq(a: real, b: real): boolean => a = b
                routine ne(a: real, b: real): boolean => a /= b
                routine main() is
                end
                """;
            Class<?> clazz = compileToClass(code, "TestNaN");
            double nan = Double.NaN;
            boolean gt = (boolean) clazz.getMethod("gt", double.class, double.class).invoke(null, nan, 1.0);
            boolean ge = (boolean) clazz.getMethod("ge", double.class, double.class).invoke(null, nan, 1.0);
            boolean lt = (boolean) clazz.getMethod("lt", double.class, double.class).invoke(null, nan, 1.0);
            boolean le = (boolean) clazz.getMethod("le", double.class, double.class).invoke(null, nan, 1.0);
            boolean eq = (boolean) clazz.getMethod("eq", double.class, double.class).invoke(null, nan, nan);
            boolean ne = (boolean) clazz.getMethod("ne", double.class, double.class).invoke(null, nan, nan);
            boolean ordinary = (boolean) clazz.getMethod("gt", double.class, double.class).invoke(null, 2.0, 1.0)
                    && (boolean) clazz.getMethod("le", double.class, double.class).invoke(null, 1.0, 1.0);
            check("Real comparisons with NaN follow IEEE 754", !gt && !ge && !lt && !le && !eq && ne && ordinary,
                    "Got >:" + gt + " >=:" + ge + " <:" + lt + " <=:" + le + " =:" + eq + " /=:" + ne
                            + " ordinary:" + ordinary);
        } catch (Exception e) {
            check("Real comparisons with NaN follow IEEE 754", false, e.getMessage());
        }
    }

    private static void testStringDefaultValue() {
        try {
            String code = """
                var globalText: string
                routine localIsEmpty(): boolean is
                    var s: string
                    return s = ""
                end
                routine main() is
                end
                """;
            Class<?> clazz = compileToClass(code, "TestStringDefault");
            Object global = clazz.getField("globalText").get(null);
            boolean local = (boolean) clazz.getMethod("localIsEmpty").invoke(null);
            check("Uninitialized string defaults to empty string", "".equals(global) && local,
                    "Expected global=\"\" and local=true, got " + global + ", " + local);
        } catch (Exception e) {
            check("Uninitialized string defaults to empty string", false, e.getMessage());
        }
    }

    private static void testScopedSlotReuse() {
        LocalVariableIndexManager slots = new LocalVariableIndexManager();
        Symbol param = new VariableSymbol("p", null, null, false);
        Symbol inner = new VariableSymbol("inner", null, null, false);
        Symbol sibling = new VariableSymbol("sibling", null, null, false);

        int paramSlot = slots.allocateSlot(param, PrimitiveType.INTEGER);
        slots.enterScope();
        int innerSlot = slots.allocateSlot(inner, PrimitiveType.REAL);
        slots.exitScope();
        boolean innerReleased = !slots.hasSlot(inner) && slots.getNextSlotIndex() == 1;
        slots.enterScope();
        int siblingSlot = slots.allocateSlot(sibling, PrimitiveType.INTEGER);
        slots.exitScope();

        check("Block-scoped local slots are released and reused",
                paramSlot == 0 && innerSlot == 1 && siblingSlot == 1 && innerReleased && slots.hasSlot(param),
                "Got param=" + paramSlot + " inner=" + innerSlot + " sibling=" + siblingSlot
                        + " released=" + innerReleased);
    }

    private static void testBlockLocalsExecution() {
        try {
            String code = """
                routine pick(n: integer): real is
                    var base is 1.5
                    if n > 0 then
                        var a is 2.5
                        var k is n * 2
                        base := base + a + k as real
                    else
                        var b is 10
                        if b > n then
                            var c is 0.25
                            base := base - c
                        end
                    end
                    var tail is 100
                    return base + tail as real
                end
                routine main() is
                end
                """;
            Class<?> clazz = compileToClass(code, "TestBlockLocals");
            Method pick = clazz.getMethod("pick", int.class);
            double pos = (double) pick.invoke(null, 3);
            double neg = (double) pick.invoke(null, -1);
            check("Block-local variables with reused slots execute correctly",
                    Math.abs(pos - 110.0) < 1e-9 && Math.abs(neg - 101.25) < 1e-9,
                    "Expected 110.0 and 101.25, got " + pos + ", " + neg);
        } catch (Exception e) {
            check("Block-local variables with reused slots execute correctly", false, e.getMessage());
        }
    }
}
