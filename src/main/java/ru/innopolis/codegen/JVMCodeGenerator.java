package ru.innopolis.codegen;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import ru.innopolis.ast.Declaration;
import ru.innopolis.ast.ProgramNode;
import ru.innopolis.ast.declaration.ParameterNode;
import ru.innopolis.ast.declaration.RoutineDeclarationNode;
import ru.innopolis.ast.declaration.TypeDeclarationNode;
import ru.innopolis.ast.declaration.VariableDeclarationNode;
import ru.innopolis.ast.expression.*;
import ru.innopolis.ast.statement.*;
import ru.innopolis.ast.type.ArrayTypeNode;
import ru.innopolis.ast.type.NamedTypeNode;
import ru.innopolis.ast.type.PrimitiveTypeNode;
import ru.innopolis.ast.type.RecordTypeNode;
import ru.innopolis.codegen.context.CodegenContext;
import ru.innopolis.codegen.context.LocalVariableIndexManager;
import ru.innopolis.codegen.type.JVMTypeMapper;
import ru.innopolis.lexer.TokenType;
import ru.innopolis.semantic.AnalysisResult;
import ru.innopolis.semantic.symbol.Symbol;
import ru.innopolis.semantic.symbol.VariableSymbol;
import ru.innopolis.semantic.type.*;
import ru.innopolis.semantic.visitor.ASTVisitor;
import ru.innopolis.semantic.visitor.BaseASTVisitor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * JVM Bytecode Generator implementing ASTVisitor.
 * Generates valid JVM .class bytecode for programs, routines, and expressions.
 * Core Architecture & Expression Engine implemented by Person 1 (Valerii Tiniakov).
 */
public class JVMCodeGenerator implements ASTVisitor<Void, CodegenContext> {
    private final Map<RecordType, List<Symbol>> constructorParameters = new HashMap<>();
    private final Map<Symbol, RecordType> constructorFields = new HashMap<>();

    private ClassWriter newClassWriter(CodegenContext context) {
        String recordPrefix = context.getProgramClassName() + "$Record_";
        return new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS) {
            @Override
            protected String getCommonSuperClass(String first, String second) {
                // Generated records extend Object and are not yet available to ASM's loader.
                if (first.startsWith(recordPrefix) || second.startsWith(recordPrefix)) return "java/lang/Object";
                return super.getCommonSuperClass(first, second);
            }
        };
    }

    private void generateRecord(RecordType record, RecordTypeNode declaration, CodegenContext context) {
        String program = context.getProgramClassName();
        String owner = JVMTypeMapper.toInternalName(record, program);
        ClassWriter writer = newClassWriter(context);
        writer.visit(Opcodes.V21, Opcodes.ACC_PUBLIC | Opcodes.ACC_SUPER, owner, null, "java/lang/Object", null);
        record.getFields().forEach((name, type) -> writer.visitField(Opcodes.ACC_PUBLIC, name,
                JVMTypeMapper.toDescriptor(type, program), null, null).visitEnd());

        List<Symbol> parameters = recordParameters(record, context);
        MethodVisitor mv = writer.visitMethod(Opcodes.ACC_PUBLIC, "<init>", constructorDescriptor(parameters, context), null, null);
        LocalVariableIndexManager locals = new LocalVariableIndexManager(1);
        for (Symbol parameter : parameters) locals.allocateSlot(parameter, context.getSymbolType(parameter));
        context.setLocalIndexManager(locals);
        context.setCurrentMethodVisitor(mv);
        mv.visitCode();
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
        for (VariableDeclarationNode field : declaration.getFields()) {
            Type type = record.getField(field.getName());
            mv.visitVarInsn(Opcodes.ALOAD, 0);
            if (field.getInitializer() == null) emitDefaultValue(type, context);
            else field.getInitializer().accept(this, context);
            mv.visitFieldInsn(Opcodes.PUTFIELD, owner, field.getName(), JVMTypeMapper.toDescriptor(type, program));
            constructorFields.put(context.getSemanticContext().getDeclaredSymbol(field), record);
        }
        mv.visitInsn(Opcodes.RETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
        writer.visitEnd();
        context.addGeneratedClass(owner, writer.toByteArray());
        constructorFields.clear();
        context.setLocalIndexManager(null);
        context.setCurrentMethodVisitor(null);
    }

    // Local record declarations may use enclosing variables in initializers or array bounds.
    private List<Symbol> recordParameters(RecordType record, CodegenContext context) {
        if (constructorParameters.containsKey(record)) return constructorParameters.get(record);
        Set<Symbol> parameters = new LinkedHashSet<>();
        var declaration = context.getSemanticContext().getRecords().get(record);
        BaseASTVisitor<Void, Void> references = new BaseASTVisitor<>() {
            @Override
            public Void visit(VariableReferenceNode reference, Void unused) {
                Symbol symbol = context.getResolvedSymbol(reference);
                if (!context.isGlobalVariable(symbol)) parameters.add(symbol);
                return null;
            }
        };
        for (VariableDeclarationNode field : declaration.getFields()) {
            if (field.getInitializer() != null) {
                field.getInitializer().accept(references, null);
                continue;
            }
            Type type = record.getField(field.getName());
            if (type instanceof RecordType nested) parameters.addAll(recordParameters(nested, context));
            while (type instanceof ArrayType array && array.isSized()) {
                if (array.getSize() == null) array.getSizeExpression().accept(references, null);
                type = array.getElementType();
            }
        }
        for (VariableDeclarationNode field : declaration.getFields()) {
            parameters.remove(context.getSemanticContext().getDeclaredSymbol(field));
        }
        List<Symbol> result = List.copyOf(parameters);
        constructorParameters.put(record, result);
        return result;
    }

    private String constructorDescriptor(List<Symbol> parameters, CodegenContext context) {
        return JVMTypeMapper.toMethodDescriptor(parameters.stream().map(context::getSymbolType).toList(),
                VoidType.INSTANCE, context.getProgramClassName());
    }

    /**
     * Compiles an AnalysisResult into a map of generated class bytecodes.
     * Entry point for compiling a semantically verified program.
     */
    public static Map<String, byte[]> compile(AnalysisResult analysisResult, String programClassName) {
        if (!analysisResult.isSuccess()) {
            throw new IllegalArgumentException("Cannot generate bytecode for a program with semantic errors.");
        }

        CodegenContext context = new CodegenContext(programClassName, analysisResult.getContext());
        JVMCodeGenerator generator = new JVMCodeGenerator();
        analysisResult.getProgram().accept(generator, context);
        return context.getGeneratedClasses();
    }

    // =========================================================================
    // 1. PROGRAM AND CLASS STRUCTURE GENERATION (Person 1)
    // =========================================================================

    @Override
    public Void visit(ProgramNode node, CodegenContext context) {
        ClassWriter cw = newClassWriter(context);
        context.setCurrentClassWriter(cw);

        String internalClassName = context.getProgramClassName();
        cw.visit(Opcodes.V21,
                Opcodes.ACC_PUBLIC | Opcodes.ACC_SUPER,
                internalClassName,
                null,
                "java/lang/Object",
                null);

        // 1.1 Generate default constructor <init>()V
        generateDefaultConstructor(cw);

        // 1.2 Pre-scan and register all routines and global variables
        List<VariableDeclarationNode> globalVars = new ArrayList<>();
        List<RoutineDeclarationNode> routines = new ArrayList<>();

        for (Declaration decl : node.getDeclarations()) {
            if (decl instanceof VariableDeclarationNode varDecl) {
                globalVars.add(varDecl);
                Symbol sym = context.getSemanticContext().getDeclaredSymbol(varDecl);
                if (sym instanceof VariableSymbol varSym) {
                    Type type = context.getSymbolType(varSym);
                    context.registerGlobalVariable(varSym, varDecl.getName());
                    String desc = JVMTypeMapper.toDescriptor(type, internalClassName);
                    cw.visitField(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, varDecl.getName(), desc, null, null).visitEnd();
                }
            } else if (decl instanceof RoutineDeclarationNode routineDecl) {
                routines.add(routineDecl);
                registerRoutineSignature(routineDecl, context);
            }
        }

        context.getSemanticContext().getRecords().forEach((record, fields) -> generateRecord(record, fields, context));
        // 1.3 Generate static initializer <clinit>()V for global variables
        generateStaticInitializer(globalVars, context);

        // 1.4 Generate standard JVM entry point public static void main(String[] args)
        generateMainEntryPoint(context);

        // 1.5 Visit all routine declarations
        for (RoutineDeclarationNode routine : routines) {
            routine.accept(this, context);
        }

        cw.visitEnd();
        context.addGeneratedClass(internalClassName, cw.toByteArray());
        return null;
    }

    private void generateDefaultConstructor(ClassWriter cw) {
        MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
        mv.visitCode();
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
        mv.visitInsn(Opcodes.RETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }

    private void registerRoutineSignature(RoutineDeclarationNode routine, CodegenContext context) {
        if (routine.isForwardDeclaration()) return;
        List<Type> paramTypes = new ArrayList<>();
        for (ParameterNode p : routine.getParameters()) {
            Symbol sym = context.getSemanticContext().getDeclaredSymbol(p);
            paramTypes.add(context.getSymbolType(sym));
        }

        Type retType = context.getSymbolType(context.getSemanticContext().getDeclaredSymbol(routine));

        String descriptor = JVMTypeMapper.toMethodDescriptor(paramTypes, retType, context.getProgramClassName());
        context.registerRoutineDescriptor(routine.getName(), descriptor);
    }

    private void generateStaticInitializer(List<VariableDeclarationNode> globalVars, CodegenContext context) {
        if (globalVars.isEmpty()) return;

        ClassWriter cw = context.getCurrentClassWriter();
        MethodVisitor mv = cw.visitMethod(Opcodes.ACC_STATIC, "<clinit>", "()V", null, null);
        mv.visitCode();
        context.setCurrentMethodVisitor(mv);

        for (VariableDeclarationNode varDecl : globalVars) {
            Symbol sym = context.getSemanticContext().getDeclaredSymbol(varDecl);
            Type type = context.getSymbolType(sym);
            String desc = JVMTypeMapper.toDescriptor(type, context.getProgramClassName());

            if (varDecl.getInitializer() != null) {
                varDecl.getInitializer().accept(this, context);
            } else {
                emitDefaultValue(type, context);
            }
            mv.visitFieldInsn(Opcodes.PUTSTATIC, context.getProgramClassName(), varDecl.getName(), desc);
        }

        mv.visitInsn(Opcodes.RETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
        context.setCurrentMethodVisitor(null);
    }

    private void generateMainEntryPoint(CodegenContext context) {
        String mainDesc = context.getRoutineDescriptor("main");
        if (mainDesc == null) return; // Verified by Pass 1, but safe-check

        ClassWriter cw = context.getCurrentClassWriter();
        MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC,
                "main",
                "([Ljava/lang/String;)V",
                null,
                null);
        mv.visitCode();

        // Invoke user's routine main()
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, context.getProgramClassName(), "main", mainDesc, false);

        // If routine main() returns a value (e.g. integer), pop it
        int resultSlots = org.objectweb.asm.Type.getReturnType(mainDesc).getSize();
        if (resultSlots > 0) {
            mv.visitInsn(resultSlots == 2 ? Opcodes.POP2 : Opcodes.POP);
        }

        mv.visitInsn(Opcodes.RETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }

    // =========================================================================
    // 2. ROUTINE AND DECLARATION GENERATION (Person 1)
    // =========================================================================

    @Override
    public Void visit(RoutineDeclarationNode node, CodegenContext context) {
        if (node.isForwardDeclaration()) {
            return null; // Skip unfulfilled or forward prototype; body compiled in definition
        }

        String descriptor = context.getRoutineDescriptor(node.getName());
        ClassWriter cw = context.getCurrentClassWriter();
        MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC,
                node.getName(),
                descriptor,
                null,
                null);
        mv.visitCode();
        context.setCurrentMethodVisitor(mv);

        // Initialize local slots for this routine
        LocalVariableIndexManager slotManager = new LocalVariableIndexManager();
        context.setLocalIndexManager(slotManager);

        // Allocate slots for parameters
        for (ParameterNode p : node.getParameters()) {
            Symbol sym = context.getSemanticContext().getDeclaredSymbol(p);
            Type type = context.getSymbolType(sym);
            slotManager.allocateSlot(sym, type);
        }

        // Generate routine body
        if (node.getExpressionBody() != null) {
            node.getExpressionBody().accept(this, context);
            Type retType = context.getExpressionType(node.getExpressionBody());
            emitReturnInsn(retType, mv);
        } else if (node.getBody() != null) {
            node.getBody().accept(this, context);
            // Ensure void routines always end with RETURN instruction
            if (descriptor.endsWith("V")) {
                mv.visitInsn(Opcodes.RETURN);
            }
        }

        mv.visitMaxs(0, 0);
        mv.visitEnd();

        context.setCurrentMethodVisitor(null);
        context.setLocalIndexManager(null);
        return null;
    }

    @Override
    public Void visit(VariableDeclarationNode node, CodegenContext context) {
        MethodVisitor mv = context.getCurrentMethodVisitor();
        if (mv == null) return null; // Global variables handled in <clinit>

        Symbol sym = context.getSemanticContext().getDeclaredSymbol(node);
        Type type = context.getSymbolType(sym);
        int slot = context.allocateLocalSlot(sym, type);

        if (node.getInitializer() != null) {
            node.getInitializer().accept(this, context);
        } else {
            emitDefaultValue(type, context);
        }

        emitStoreInsn(type, slot, mv);
        return null;
    }

    @Override
    public Void visit(TypeDeclarationNode node, CodegenContext context) {
        // Record classes are emitted once from their resolved declarations.
        return null;
    }

    @Override
    public Void visit(ParameterNode node, CodegenContext context) {
        return null; // Handled during routine parameter binding
    }

    // =========================================================================
    // 3. EXPRESSION GENERATION (Person 1 Core Specialty)
    // =========================================================================

    @Override
    public Void visit(IntLiteralNode node, CodegenContext context) {
        emitIntConstant(node.getValue(), context.getCurrentMethodVisitor());
        return null;
    }

    @Override
    public Void visit(RealLiteralNode node, CodegenContext context) {
        MethodVisitor mv = context.getCurrentMethodVisitor();
        double val = node.getValue();
        if (Double.compare(val, 0.0) == 0) {
            mv.visitInsn(Opcodes.DCONST_0);
        } else if (val == 1.0) {
            mv.visitInsn(Opcodes.DCONST_1);
        } else {
            mv.visitLdcInsn(val);
        }
        return null;
    }

    @Override
    public Void visit(BoolLiteralNode node, CodegenContext context) {
        MethodVisitor mv = context.getCurrentMethodVisitor();
        mv.visitInsn(node.getValue() ? Opcodes.ICONST_1 : Opcodes.ICONST_0);
        return null;
    }

    @Override
    public Void visit(StringLiteralNode node, CodegenContext context) {
        context.getCurrentMethodVisitor().visitLdcInsn(node.getValue());
        return null;
    }

    @Override
    public Void visit(VariableReferenceNode node, CodegenContext context) {
        emitVariableLoad(context.getResolvedSymbol(node), context);
        return null;
    }

    private void emitVariableLoad(Symbol sym, CodegenContext context) {
        MethodVisitor mv = context.getCurrentMethodVisitor();
        Type type = context.getSymbolType(sym);

        if (context.isGlobalVariable(sym)) {
            String fieldName = context.getGlobalFieldName(sym);
            String desc = JVMTypeMapper.toDescriptor(type, context.getProgramClassName());
            mv.visitFieldInsn(Opcodes.GETSTATIC, context.getProgramClassName(), fieldName, desc);
        } else if (constructorFields.containsKey(sym)) {
            mv.visitVarInsn(Opcodes.ALOAD, 0);
            mv.visitFieldInsn(Opcodes.GETFIELD, JVMTypeMapper.toInternalName(constructorFields.get(sym),
                    context.getProgramClassName()), sym.getName(), JVMTypeMapper.toDescriptor(type, context.getProgramClassName()));
        } else {
            int slot = context.getLocalSlot(sym);
            emitLoadInsn(type, slot, mv);
        }
    }

    @Override
    public Void visit(BinaryExpressionNode node, CodegenContext context) {
        MethodVisitor mv = context.getCurrentMethodVisitor();
        TokenType op = node.getOperator();
        Type leftType = context.getExpressionType(node.getLeft());
        Type resultType = context.getExpressionType(node);

        // 3.1 Arithmetic Operations (+, -, *, /, %)
        if (op == TokenType.PLUS || op == TokenType.MINUS || op == TokenType.MULT ||
                op == TokenType.DIV || op == TokenType.MOD) {
            node.getLeft().accept(this, context);
            node.getRight().accept(this, context);

            if (resultType == PrimitiveType.INTEGER) {
                switch (op) {
                    case PLUS -> mv.visitInsn(Opcodes.IADD);
                    case MINUS -> mv.visitInsn(Opcodes.ISUB);
                    case MULT -> mv.visitInsn(Opcodes.IMUL);
                    case DIV -> mv.visitInsn(Opcodes.IDIV);
                    case MOD -> mv.visitInsn(Opcodes.IREM);
                    default -> throw new UnsupportedOperationException("Unknown integer op: " + op);
                }
            } else if (resultType == PrimitiveType.REAL) {
                switch (op) {
                    case PLUS -> mv.visitInsn(Opcodes.DADD);
                    case MINUS -> mv.visitInsn(Opcodes.DSUB);
                    case MULT -> mv.visitInsn(Opcodes.DMUL);
                    case DIV -> mv.visitInsn(Opcodes.DDIV);
                    default -> throw new UnsupportedOperationException("Unknown real op: " + op);
                }
            }
            return null;
        }

        // 3.2 Logical Operators (and, or, xor)
        if (op == TokenType.AND || op == TokenType.OR || op == TokenType.XOR) {
            node.getLeft().accept(this, context);
            node.getRight().accept(this, context);
            switch (op) {
                case AND -> mv.visitInsn(Opcodes.IAND);
                case OR -> mv.visitInsn(Opcodes.IOR);
                case XOR -> mv.visitInsn(Opcodes.IXOR);
            }
            return null;
        }

        // 3.3 Relational Comparisons (<, <=, >, >=, =, /=)
        emitComparison(node.getLeft(), node.getRight(), op, leftType, mv, context);
        return null;
    }

    private void emitComparison(Expression left, Expression right, TokenType op, Type operandType,
                                MethodVisitor mv, CodegenContext context) {
        left.accept(this, context);
        right.accept(this, context);

        Label labelTrue = new Label();
        Label labelEnd = new Label();

        if (operandType == PrimitiveType.INTEGER || operandType == PrimitiveType.BOOLEAN) {
            int jumpOpcode = switch (op) {
                case LESS -> Opcodes.IF_ICMPLT;
                case LESS_EQUAL -> Opcodes.IF_ICMPLE;
                case GREATER -> Opcodes.IF_ICMPGT;
                case GREATER_EQUAL -> Opcodes.IF_ICMPGE;
                case EQUAL -> Opcodes.IF_ICMPEQ;
                case NOT_EQUAL -> Opcodes.IF_ICMPNE;
                default -> throw new IllegalArgumentException("Unknown comparison: " + op);
            };
            mv.visitJumpInsn(jumpOpcode, labelTrue);
        } else if (operandType == PrimitiveType.REAL) {
            boolean lessFamily = op == TokenType.LESS || op == TokenType.LESS_EQUAL;
            mv.visitInsn(lessFamily ? Opcodes.DCMPG : Opcodes.DCMPL);
            int jumpOpcode = switch (op) {
                case LESS -> Opcodes.IFLT;
                case LESS_EQUAL -> Opcodes.IFLE;
                case GREATER -> Opcodes.IFGT;
                case GREATER_EQUAL -> Opcodes.IFGE;
                case EQUAL -> Opcodes.IFEQ;
                case NOT_EQUAL -> Opcodes.IFNE;
                default -> throw new IllegalArgumentException("Unknown real comparison: " + op);
            };
            mv.visitJumpInsn(jumpOpcode, labelTrue);
        } else if (operandType == PrimitiveType.STRING) {
            mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/String", "equals", "(Ljava/lang/Object;)Z", false);
            if (op == TokenType.NOT_EQUAL) {
                // Invert result for /=
                mv.visitInsn(Opcodes.ICONST_1);
                mv.visitInsn(Opcodes.IXOR);
            }
            return;
        } else {
            // Reference equality (=, /=)
            int jumpOpcode = (op == TokenType.EQUAL) ? Opcodes.IF_ACMPEQ : Opcodes.IF_ACMPNE;
            mv.visitJumpInsn(jumpOpcode, labelTrue);
        }

        // False branch: push 0
        mv.visitInsn(Opcodes.ICONST_0);
        mv.visitJumpInsn(Opcodes.GOTO, labelEnd);

        // True branch: push 1
        mv.visitLabel(labelTrue);
        mv.visitInsn(Opcodes.ICONST_1);

        mv.visitLabel(labelEnd);
    }

    @Override
    public Void visit(UnaryExpressionNode node, CodegenContext context) {
        MethodVisitor mv = context.getCurrentMethodVisitor();
        node.getOperand().accept(this, context);
        Type operandType = context.getExpressionType(node.getOperand());

        if (node.getOperator() == TokenType.MINUS) {
            if (operandType == PrimitiveType.INTEGER) {
                mv.visitInsn(Opcodes.INEG);
            } else if (operandType == PrimitiveType.REAL) {
                mv.visitInsn(Opcodes.DNEG);
            }
        } else if (node.getOperator() == TokenType.NOT) {
            // Invert boolean 0 <-> 1
            mv.visitInsn(Opcodes.ICONST_1);
            mv.visitInsn(Opcodes.IXOR);
        }
        // Unary PLUS is a no-op
        return null;
    }

    @Override
    public Void visit(CastExpressionNode node, CodegenContext context) {
        MethodVisitor mv = context.getCurrentMethodVisitor();
        node.getExpression().accept(this, context);

        Type fromType = context.getExpressionType(node.getExpression());
        Type targetType = context.getExpressionType(node);

        if ((fromType == PrimitiveType.INTEGER || fromType == PrimitiveType.BOOLEAN)
                && targetType == PrimitiveType.REAL) {
            mv.visitInsn(Opcodes.I2D);
        } else if (fromType == PrimitiveType.REAL && targetType == PrimitiveType.INTEGER) {
            // Vitebsk specification: copying with rounding to nearest integer
            mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Math", "round", "(D)J", false);
            mv.visitInsn(Opcodes.L2I);
        } else if (fromType == PrimitiveType.BOOLEAN && targetType == PrimitiveType.INTEGER) {
            // No-op: in JVM boolean is represented as int (0 or 1)
        } else if (fromType == PrimitiveType.INTEGER && targetType == PrimitiveType.BOOLEAN) {
            Label valid = new Label();
            mv.visitInsn(Opcodes.DUP);
            mv.visitJumpInsn(Opcodes.IFEQ, valid);
            mv.visitInsn(Opcodes.DUP);
            mv.visitInsn(Opcodes.ICONST_1);
            mv.visitJumpInsn(Opcodes.IF_ICMPEQ, valid);
            mv.visitInsn(Opcodes.POP);
            mv.visitTypeInsn(Opcodes.NEW, "java/lang/IllegalArgumentException");
            mv.visitInsn(Opcodes.DUP);
            mv.visitLdcInsn("integer as boolean requires 0 or 1");
            mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/IllegalArgumentException", "<init>",
                    "(Ljava/lang/String;)V", false);
            mv.visitInsn(Opcodes.ATHROW);
            mv.visitLabel(valid);
        }
        return null;
    }

    @Override
    public Void visit(RoutineCallExpressionNode node, CodegenContext context) {
        MethodVisitor mv = context.getCurrentMethodVisitor();

        // 1. Evaluate arguments onto stack left-to-right
        for (Expression arg : node.getArguments()) {
            arg.accept(this, context);
        }

        // 2. Invoke static routine method
        String descriptor = context.getRoutineDescriptor(node.getRoutineName());
        if (descriptor == null) {
            throw new IllegalStateException("Undefined routine descriptor for: " + node.getRoutineName());
        }
        mv.visitMethodInsn(Opcodes.INVOKESTATIC,
                context.getProgramClassName(),
                node.getRoutineName(),
                descriptor,
                false);
        return null;
    }

    // =========================================================================
    // 4. STATEMENTS BASELINE (Person 1 baseline & hooks for Person 2 / Person 3)
    // =========================================================================

    @Override
    public Void visit(BodyNode node, CodegenContext context) {
        LocalVariableIndexManager slots = context.getLocalIndexManager();
        if (slots != null) {
            slots.enterScope();
        }
        for (ru.innopolis.ast.ASTNode item : node.getItems()) {
            item.accept(this, context);
        }
        if (slots != null) {
            slots.exitScope();
        }
        return null;
    }

    @Override
    public Void visit(ReturnStatementNode node, CodegenContext context) {
        MethodVisitor mv = context.getCurrentMethodVisitor();
        if (node.getValue() != null) {
            node.getValue().accept(this, context);
            Type retType = context.getExpressionType(node.getValue());
            emitReturnInsn(retType, mv);
        } else {
            mv.visitInsn(Opcodes.RETURN);
        }
        return null;
    }

    @Override
    public Void visit(RoutineCallStatementNode node, CodegenContext context) {
        MethodVisitor mv = context.getCurrentMethodVisitor();
        node.getCall().accept(this, context);

        // If function returns a value, discard it from the stack
        Type retType = context.getExpressionType(node.getCall());
        if (retType != null && !(retType instanceof VoidType)) {
            if (retType == PrimitiveType.REAL) {
                mv.visitInsn(Opcodes.POP2);
            } else {
                mv.visitInsn(Opcodes.POP);
            }
        }
        return null;
    }

    @Override
    public Void visit(AssignmentNode node, CodegenContext context) {
        MethodVisitor mv = context.getCurrentMethodVisitor();

        // Basic variable assignment
        if (node.getTarget() instanceof VariableReferenceNode varRef) {
            node.getValue().accept(this, context);
            Symbol sym = context.getResolvedSymbol(varRef);
            Type type = context.getSymbolType(sym);

            if (context.isGlobalVariable(sym)) {
                String fieldName = context.getGlobalFieldName(sym);
                String desc = JVMTypeMapper.toDescriptor(type, context.getProgramClassName());
                mv.visitFieldInsn(Opcodes.PUTSTATIC, context.getProgramClassName(), fieldName, desc);
            } else {
                int slot = context.getLocalSlot(sym);
                emitStoreInsn(type, slot, mv);
            }
            return null;
        }

        if (node.getTarget() instanceof ArrayAccessNode element) {
            element.getArray().accept(this, context);
            element.getIndex().accept(this, context);
            node.getValue().accept(this, context);
            mv.visitInsn(arrayOpcode(context.getExpressionType(element), Opcodes.IASTORE, context));
        } else if (node.getTarget() instanceof RecordAccessNode field) {
            field.getRecord().accept(this, context);
            node.getValue().accept(this, context);
            emitFieldAccess(field, Opcodes.PUTFIELD, context);
        } else {
            throw new IllegalArgumentException("Invalid assignment target: " + node.getTarget());
        }
        return null;
    }

    @Override
    public Void visit(IfStatementNode node, CodegenContext context) {
        MethodVisitor mv = context.getCurrentMethodVisitor();
        BodyNode elseBody = node.getElseBody();
        Label labelEnd = new Label();
        Label labelFalse = elseBody == null ? labelEnd : new Label();

        node.getCondition().accept(this, context);
        mv.visitJumpInsn(Opcodes.IFEQ, labelFalse);
        node.getThenBody().accept(this, context);
        if (elseBody != null) {
            mv.visitJumpInsn(Opcodes.GOTO, labelEnd);
            mv.visitLabel(labelFalse);
            elseBody.accept(this, context);
        }

        mv.visitLabel(labelEnd);
        return null;
    }

    @Override
    public Void visit(WhileLoopNode node, CodegenContext context) {
        MethodVisitor mv = context.getCurrentMethodVisitor();
        Label labelStart = new Label();
        Label labelEnd = new Label();

        mv.visitLabel(labelStart);
        node.getCondition().accept(this, context);
        mv.visitJumpInsn(Opcodes.IFEQ, labelEnd);

        node.getBody().accept(this, context);
        mv.visitJumpInsn(Opcodes.GOTO, labelStart);

        mv.visitLabel(labelEnd);
        return null;
    }

    @Override
    public Void visit(ForLoopNode node, CodegenContext context) {
        MethodVisitor mv = context.getCurrentMethodVisitor();
        LocalVariableIndexManager locals = context.getLocalIndexManager();
        locals.enterScope();
        RangeNode range = node.getRange();
        Symbol loopVariable = context.getSemanticContext().getDeclaredSymbol(node);
        Type elementType = context.getSymbolType(loopVariable);
        int variableSlot = context.allocateLocalSlot(loopVariable, elementType);
        boolean arrayIteration = range.isArrayIteration();
        int counterSlot = arrayIteration ? locals.allocateTemporarySlot(PrimitiveType.INTEGER) : variableSlot;
        int limitSlot = locals.allocateTemporarySlot(PrimitiveType.INTEGER);
        boolean reverse = node.isReverse();
        int lowerSlot = reverse ? limitSlot : counterSlot;
        int upperSlot = reverse ? counterSlot : limitSlot;

        range.accept(this, context);
        int arraySlot = -1;
        if (arrayIteration) {
            arraySlot = locals.allocateTemporarySlot(context.getExpressionType(range.getStart()));
            mv.visitVarInsn(Opcodes.ASTORE, arraySlot);
            mv.visitInsn(Opcodes.ICONST_0);
            mv.visitVarInsn(Opcodes.ISTORE, lowerSlot);
            mv.visitVarInsn(Opcodes.ALOAD, arraySlot);
            mv.visitInsn(Opcodes.ARRAYLENGTH);
            mv.visitInsn(Opcodes.ICONST_1);
            mv.visitInsn(Opcodes.ISUB);
        }
        mv.visitVarInsn(Opcodes.ISTORE, upperSlot);
        if (!arrayIteration) {
            mv.visitVarInsn(Opcodes.ISTORE, lowerSlot);
        }

        Label body = new Label();
        Label end = new Label();
        mv.visitVarInsn(Opcodes.ILOAD, lowerSlot);
        mv.visitVarInsn(Opcodes.ILOAD, upperSlot);
        mv.visitJumpInsn(Opcodes.IF_ICMPGT, end);

        mv.visitLabel(body);
        if (arrayIteration) {
            mv.visitVarInsn(Opcodes.ALOAD, arraySlot);
            mv.visitVarInsn(Opcodes.ILOAD, counterSlot);
            mv.visitInsn(arrayOpcode(elementType, Opcodes.IALOAD, context));
            emitStoreInsn(elementType, variableSlot, mv);
        }
        node.getBody().accept(this, context);

        // Stop before stepping past the last value, including Integer.MIN_VALUE and MAX_VALUE.
        mv.visitVarInsn(Opcodes.ILOAD, counterSlot);
        mv.visitVarInsn(Opcodes.ILOAD, limitSlot);
        mv.visitJumpInsn(Opcodes.IF_ICMPEQ, end);
        mv.visitIincInsn(counterSlot, reverse ? -1 : 1);
        mv.visitJumpInsn(Opcodes.GOTO, body);
        mv.visitLabel(end);
        locals.exitScope();
        return null;
    }

    @Override
    public Void visit(RangeNode node, CodegenContext context) {
        node.getStart().accept(this, context);
        if (!node.isArrayIteration()) {
            node.getEnd().accept(this, context);
        }
        return null;
    }

    @Override
    public Void visit(PrintStatementNode node, CodegenContext context) {
        // Print the arguments in order and terminate the statement with one newline.
        MethodVisitor mv = context.getCurrentMethodVisitor();
        for (Expression expr : node.getExpressions()) {
            mv.visitFieldInsn(Opcodes.GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
            expr.accept(this, context);
            Type type = context.getExpressionType(expr);
            String desc = "(" + JVMTypeMapper.toDescriptor(type, context.getProgramClassName()) + ")V";
            mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/io/PrintStream", "print", desc, false);
        }
        mv.visitFieldInsn(Opcodes.GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/io/PrintStream", "println", "()V", false);
        return null;
    }

    // =========================================================================
    // 5. ARRAY ELEMENTS AND RECORD FIELDS
    // =========================================================================

    @Override
    public Void visit(ArrayAccessNode node, CodegenContext context) {
        node.getArray().accept(this, context);
        node.getIndex().accept(this, context);
        context.getCurrentMethodVisitor().visitInsn(arrayOpcode(context.getExpressionType(node), Opcodes.IALOAD, context));
        return null;
    }

    @Override
    public Void visit(RecordAccessNode node, CodegenContext context) {
        node.getRecord().accept(this, context);
        if (context.getExpressionType(node.getRecord()) instanceof ArrayType) {
            context.getCurrentMethodVisitor().visitInsn(Opcodes.ARRAYLENGTH);
        } else {
            emitFieldAccess(node, Opcodes.GETFIELD, context);
        }
        return null;
    }

    private int arrayOpcode(Type element, int integerOpcode, CodegenContext context) {
        String descriptor = JVMTypeMapper.toDescriptor(element, context.getProgramClassName());
        return org.objectweb.asm.Type.getType(descriptor).getOpcode(integerOpcode);
    }

    private void emitFieldAccess(RecordAccessNode field, int opcode, CodegenContext context) {
        String owner = JVMTypeMapper.toInternalName(context.getExpressionType(field.getRecord()), context.getProgramClassName());
        String descriptor = JVMTypeMapper.toDescriptor(context.getExpressionType(field), context.getProgramClassName());
        context.getCurrentMethodVisitor().visitFieldInsn(opcode, owner, field.getFieldName(), descriptor);
    }

    @Override
    public Void visit(PrimitiveTypeNode node, CodegenContext context) {
        return null;
    }

    @Override
    public Void visit(ArrayTypeNode node, CodegenContext context) {
        return null;
    }

    @Override
    public Void visit(RecordTypeNode node, CodegenContext context) {
        return null;
    }

    @Override
    public Void visit(NamedTypeNode node, CodegenContext context) {
        return null;
    }

    // =========================================================================
    // 6. HELPER EMITTERS (Person 1)
    // =========================================================================

    private void emitIntConstant(int val, MethodVisitor mv) {
        if (val >= -1 && val <= 5) {
            mv.visitInsn(switch (val) {
                case -1 -> Opcodes.ICONST_M1;
                case 0 -> Opcodes.ICONST_0;
                case 1 -> Opcodes.ICONST_1;
                case 2 -> Opcodes.ICONST_2;
                case 3 -> Opcodes.ICONST_3;
                case 4 -> Opcodes.ICONST_4;
                case 5 -> Opcodes.ICONST_5;
                default -> throw new IllegalStateException();
            });
        } else if (val >= Byte.MIN_VALUE && val <= Byte.MAX_VALUE) {
            mv.visitIntInsn(Opcodes.BIPUSH, val);
        } else if (val >= Short.MIN_VALUE && val <= Short.MAX_VALUE) {
            mv.visitIntInsn(Opcodes.SIPUSH, val);
        } else {
            mv.visitLdcInsn(val);
        }
    }

    private void emitDefaultValue(Type type, CodegenContext context) {
        MethodVisitor mv = context.getCurrentMethodVisitor();
        if (type == PrimitiveType.INTEGER || type == PrimitiveType.BOOLEAN) {
            mv.visitInsn(Opcodes.ICONST_0);
        } else if (type == PrimitiveType.REAL) {
            mv.visitInsn(Opcodes.DCONST_0);
        } else if (type == PrimitiveType.STRING) {
            mv.visitLdcInsn("");
        } else if (type instanceof RecordType record) {
            String owner = JVMTypeMapper.toInternalName(type, context.getProgramClassName());
            mv.visitTypeInsn(Opcodes.NEW, owner);
            mv.visitInsn(Opcodes.DUP);
            List<Symbol> parameters = recordParameters(record, context);
            for (Symbol parameter : parameters) emitVariableLoad(parameter, context);
            mv.visitMethodInsn(Opcodes.INVOKESPECIAL, owner, "<init>", constructorDescriptor(parameters, context), false);
        } else if (type instanceof ArrayType array && array.isSized()) {
            emitNewArray(array, context);
        } else {
            mv.visitInsn(Opcodes.ACONST_NULL);
        }
    }

    private void emitNewArray(ArrayType array, CodegenContext context) {
        MethodVisitor mv = context.getCurrentMethodVisitor();
        int dimensions = 0;
        Type element = array;
        while (element instanceof ArrayType dimension && dimension.isSized()) {
            if (dimension.getSize() != null) emitIntConstant(dimension.getSize(), mv);
            else dimension.getSizeExpression().accept(this, context);
            dimensions++;
            element = dimension.getElementType();
        }
        if (dimensions > 1) {
            mv.visitMultiANewArrayInsn(JVMTypeMapper.toDescriptor(array, context.getProgramClassName()), dimensions);
        } else if (element == PrimitiveType.INTEGER) {
            mv.visitIntInsn(Opcodes.NEWARRAY, Opcodes.T_INT);
        } else if (element == PrimitiveType.REAL) {
            mv.visitIntInsn(Opcodes.NEWARRAY, Opcodes.T_DOUBLE);
        } else if (element == PrimitiveType.BOOLEAN) {
            mv.visitIntInsn(Opcodes.NEWARRAY, Opcodes.T_BOOLEAN);
        } else {
            mv.visitTypeInsn(Opcodes.ANEWARRAY, JVMTypeMapper.toInternalName(element, context.getProgramClassName()));
        }
    }

    private void emitLoadInsn(Type type, int slot, MethodVisitor mv) {
        if (type == PrimitiveType.INTEGER || type == PrimitiveType.BOOLEAN) {
            mv.visitVarInsn(Opcodes.ILOAD, slot);
        } else if (type == PrimitiveType.REAL) {
            mv.visitVarInsn(Opcodes.DLOAD, slot);
        } else {
            mv.visitVarInsn(Opcodes.ALOAD, slot);
        }
    }

    private void emitStoreInsn(Type type, int slot, MethodVisitor mv) {
        if (type == PrimitiveType.INTEGER || type == PrimitiveType.BOOLEAN) {
            mv.visitVarInsn(Opcodes.ISTORE, slot);
        } else if (type == PrimitiveType.REAL) {
            mv.visitVarInsn(Opcodes.DSTORE, slot);
        } else {
            mv.visitVarInsn(Opcodes.ASTORE, slot);
        }
    }

    private void emitReturnInsn(Type type, MethodVisitor mv) {
        if (type == null || type instanceof VoidType) {
            mv.visitInsn(Opcodes.RETURN);
        } else if (type == PrimitiveType.INTEGER || type == PrimitiveType.BOOLEAN) {
            mv.visitInsn(Opcodes.IRETURN);
        } else if (type == PrimitiveType.REAL) {
            mv.visitInsn(Opcodes.DRETURN);
        } else {
            mv.visitInsn(Opcodes.ARETURN);
        }
    }
}
