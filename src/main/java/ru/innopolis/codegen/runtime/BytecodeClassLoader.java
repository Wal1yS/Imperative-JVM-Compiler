package ru.innopolis.codegen.runtime;

/** Compatibility with clients of the original code generator package. */
@Deprecated
public class BytecodeClassLoader extends ru.innopolis.runtime.BytecodeClassLoader {
    public BytecodeClassLoader() { super(); }
    public BytecodeClassLoader(ClassLoader parent) { super(parent); }
}
