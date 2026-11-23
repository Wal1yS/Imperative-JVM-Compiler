package ru.innopolis.runtime;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public final class ClassFileWriter {
    private ClassFileWriter() {}

    public static void writeAll(Path outputDir, Map<String, byte[]> classes) throws IOException {
        for (var entry : classes.entrySet()) {
            Path file = outputDir.resolve(entry.getKey() + ".class");
            Files.createDirectories(file.getParent());
            Files.write(file, entry.getValue());
        }
    }
}
