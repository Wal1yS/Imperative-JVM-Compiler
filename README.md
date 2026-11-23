# Imperative-JVM-Compiler
A hand-written compiler translating a custom Imperative Language to JVM bytecode, written in Java.

Build the compiler with Maven and JDK 25. Generated programs run on Java 21 or later.

```sh
mvn clean compile
java -cp target/classes ru.innopolis.BytecodeTestRunner
java -cp target/classes ru.innopolis.Main --compile examples/14.txt
java -cp output CompiledProgram
```

`--compile` exports the program and its record classes to `output/`.
`--run` (the default) also executes the compiled program in memory.
An `integer as boolean` conversion accepts 0 (false) and 1 (true).
Any other integer raises an error when the conversion executes.
