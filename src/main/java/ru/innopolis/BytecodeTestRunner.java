package ru.innopolis;

import ru.innopolis.codegen.JVMCodeGenerator;
import ru.innopolis.lexer.Scanner;
import ru.innopolis.parser.Parser;
import ru.innopolis.runtime.BytecodeClassLoader;
import ru.innopolis.semantic.SemanticAnalyzer;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class BytecodeTestRunner {
    private static final List<String> EXPECTED = List.of(
            "Hello, Compilers Team! Project version: 1",
            "true as integer is: 1\n1 as boolean is: true\n7 / 2 in real is: 3.5\n3.5 as integer is: 4",
            "Global x is: 100\nOuter x is: 42\nInner x is: 67\nOuter x is still: 42",
            "7th Fibonacci number is: 13",
            "7th Fibonacci number is: 13",
            "Starting engine test...\nT-minus: 5\nT-minus: 4\nT-minus: 3\nT-minus: 2\nT-minus: 1\nTest complete!",
            "Sorted array smallest element is: 12\nSorted array largest element is: 64",
            "GCD of 48 and 18 is: 6",
            "Square of index 5 is: 25\nSquare of index 9 is: 81",
            "Max of 4 elements is: 42\nMax of 6 elements is: 99",
            "1\n4\n9\n16\n25",
            "Total grade points: 177",
            "Average of student 101 is: 83.75",
            "Squared length is: 25",
            "Initial boxA capacity: 10\nFinal boxA capacity after modifying boxB: 50");

    public static void main(String[] args) throws Exception {
        Path examples = Path.of(args.length == 0 ? "examples" : args[0]);
        int passed = 0;
        for (int i = 0; i < EXPECTED.size(); i++) {
            Path file = examples.resolve((i + 1) + ".txt");
            try {
                checkOutput(Files.readString(file), EXPECTED.get(i));
                System.out.println("[PASS] " + file);
                passed++;
            } catch (Exception | AssertionError | LinkageError e) {
                System.err.println("[FAIL] " + file);
                e.printStackTrace(System.err);
            }
        }
        System.out.println("Benchmarks passed: " + passed + "/" + EXPECTED.size());
        if (passed != EXPECTED.size()) throw new AssertionError("Benchmark failures");

        checkOutput("""
                routine main() is
                    var flags: array [2] boolean
                    var weights: array [2] real
                    var names: array [2] string
                    var alias is flags
                    alias[0] := true
                    weights[1] := 2.5
                    names[1] := "ok"
                    print flags[0], " ", flags[1], " ", weights[1], " ", names[1]
                end
                """, "true false 2.5 ok");
        checkOutput("""
                routine check(n: integer) is
                    type Chunk is record
                        var length: integer is n
                        var data: array [length] array [2] real
                    end
                    var a: Chunk
                    var b: Chunk
                    a.data[n - 1][1] := 4.5
                    print a.data.size, " ", a.data[n - 1][1], " ", b.data[n - 1][1]
                end
                routine main() is check(3) end
                """, "3 4.5 0.0");
        checkOutput("""
                routine flag(n: integer): boolean => n as boolean
                routine main() is print flag(0), " ", flag(1) end
                """, "false true");
        for (String expression : List.of("2 as boolean", "flag(-1)", "flag(2147483647)")) {
            String source = "routine flag(n: integer): boolean => n as boolean\n"
                    + "routine main() is var unused is " + expression + " end";
            try {
                execute(source);
                throw new AssertionError("Expected runtime error for " + expression);
            } catch (InvocationTargetException e) {
                if (!(e.getCause() instanceof IllegalArgumentException error)
                        || !"integer as boolean requires 0 or 1".equals(error.getMessage())) throw e;
            }
        }
        System.out.println("[PASS] Typed arrays, aliases and checked boolean conversions");
    }

    private static void checkOutput(String source, String expected) throws Exception {
        String actual = execute(source);
        if (!actual.equals(expected + "\n")) {
            throw new AssertionError("Expected:\n" + expected + "\nActual:\n" + actual);
        }
    }

    private static String execute(String source) throws Exception {
        var program = new Parser(new Scanner(source).scanTokens()).parseProgram();
        var analysis = SemanticAnalyzer.analyze(program);
        if (!analysis.isSuccess()) throw new AssertionError(analysis.getErrors());
        String name = "CompiledProgram";
        var loader = new BytecodeClassLoader();
        loader.addClasses(JVMCodeGenerator.compile(analysis, name));
        var output = new ByteArrayOutputStream();
        PrintStream original = System.out;
        try (var capture = new PrintStream(output, true, StandardCharsets.UTF_8)) {
            System.setOut(capture);
            loader.runMain(name);
        } finally {
            System.setOut(original);
        }
        return output.toString(StandardCharsets.UTF_8).replace("\r\n", "\n");
    }
}
