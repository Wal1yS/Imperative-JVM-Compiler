package ru.innopolis.codegen.context;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import ru.innopolis.ast.expression.Expression;
import ru.innopolis.ast.expression.RoutineCallExpressionNode;
import ru.innopolis.ast.expression.VariableReferenceNode;
import ru.innopolis.semantic.context.SemanticContext;
import ru.innopolis.semantic.symbol.RoutineSymbol;
import ru.innopolis.semantic.symbol.Symbol;
import ru.innopolis.semantic.symbol.VariableSymbol;
import ru.innopolis.semantic.type.Type;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Shared compilation context for JVM bytecode generation.
 * Coordinates ClassWriter, MethodVisitor, slot management, symbol resolution, and class artifacts.
 * Part of Person 1 (Valerii Tiniakov) foundational infrastructure.
 */
public class CodegenContext {
    private final String programClassName;
    private final SemanticContext semanticContext;

    private ClassWriter currentClassWriter;
    private MethodVisitor currentMethodVisitor;
    private LocalVariableIndexManager localIndexManager;

    private final Map<Symbol, String> globalVariableFields = new HashMap<>();
    private final Map<String, String> routineDescriptors = new HashMap<>();
    private final Map<String, byte[]> generatedClasses = new LinkedHashMap<>();

    public CodegenContext(String programClassName, SemanticContext semanticContext) {
        this.programClassName = (programClassName != null && !programClassName.isBlank())
                ? programClassName.replace('.', '/')
                : "CompiledProgram";
        this.semanticContext = semanticContext;
    }

    public String getProgramClassName() {
        return programClassName;
    }

    public SemanticContext getSemanticContext() {
        return semanticContext;
    }

    public ClassWriter getCurrentClassWriter() {
        return currentClassWriter;
    }

    public void setCurrentClassWriter(ClassWriter currentClassWriter) {
        this.currentClassWriter = currentClassWriter;
    }

    public MethodVisitor getCurrentMethodVisitor() {
        return currentMethodVisitor;
    }

    public void setCurrentMethodVisitor(MethodVisitor currentMethodVisitor) {
        this.currentMethodVisitor = currentMethodVisitor;
    }

    public LocalVariableIndexManager getLocalIndexManager() {
        return localIndexManager;
    }

    public void setLocalIndexManager(LocalVariableIndexManager localIndexManager) {
        this.localIndexManager = localIndexManager;
    }

    public void registerGlobalVariable(VariableSymbol symbol, String fieldName) {
        globalVariableFields.put(symbol, fieldName);
    }

    public boolean isGlobalVariable(Symbol symbol) {
        return globalVariableFields.containsKey(symbol);
    }

    public String getGlobalFieldName(Symbol symbol) {
        return globalVariableFields.get(symbol);
    }

    public void registerRoutineDescriptor(String routineName, String descriptor) {
        routineDescriptors.put(routineName, descriptor);
    }

    public String getRoutineDescriptor(String routineName) {
        return routineDescriptors.get(routineName);
    }

    public int allocateLocalSlot(Symbol symbol, Type type) {
        if (localIndexManager == null) {
            throw new IllegalStateException("LocalVariableIndexManager is not set for current routine");
        }
        return localIndexManager.allocateSlot(symbol, type);
    }

    public int getLocalSlot(Symbol symbol) {
        if (localIndexManager == null) {
            throw new IllegalStateException("LocalVariableIndexManager is not set for current routine");
        }
        return localIndexManager.getSlot(symbol);
    }

    public boolean hasLocalSlot(Symbol symbol) {
        return localIndexManager != null && localIndexManager.hasSlot(symbol);
    }

    public void addGeneratedClass(String internalName, byte[] bytecode) {
        generatedClasses.put(internalName, bytecode);
    }

    public Map<String, byte[]> getGeneratedClasses() {
        return Collections.unmodifiableMap(generatedClasses);
    }

    // Convenience delegates to SemanticContext
    public Type getExpressionType(Expression expr) {
        return semanticContext.getExpressionType(expr);
    }

    public Type getSymbolType(Symbol symbol) {
        return semanticContext.getSymbolType(symbol);
    }

    public Symbol getResolvedSymbol(VariableReferenceNode node) {
        return semanticContext.getResolvedSymbol(node);
    }

    public RoutineSymbol getResolvedRoutine(RoutineCallExpressionNode node) {
        return semanticContext.getResolvedRoutine(node);
    }
}
