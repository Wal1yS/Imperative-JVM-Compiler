package ru.innopolis.semantic.check;

import ru.innopolis.ast.ASTNode;
import ru.innopolis.ast.Declaration;
import ru.innopolis.ast.ProgramNode;
import ru.innopolis.ast.declaration.*;
import ru.innopolis.ast.expression.*;
import ru.innopolis.ast.statement.*;
import ru.innopolis.ast.type.NamedTypeNode;
import ru.innopolis.ast.type.RecordTypeNode;
import ru.innopolis.semantic.context.SemanticContext;
import ru.innopolis.semantic.scope.BlockScope;
import ru.innopolis.semantic.scope.GlobalScope;
import ru.innopolis.semantic.scope.IScope;
import ru.innopolis.semantic.scope.RoutineScope;
import ru.innopolis.semantic.symbol.*;
import ru.innopolis.semantic.visitor.BaseASTVisitor;

import java.util.ArrayList;
import java.util.List;

/**
 * Pass 1: Declaration & Scope Checker.
 * Implements:
 * - Curator Check 1: Correct Keyword Usage (Return scope validation, nested routine prohibition)
 * - Curator Check 2: Declarations Before Usage, Duplicate Detection & Shadowing
 * - Vitebsk Check 2: Explicit Entry Point (routine main() is ... end)
 * - Vitebsk Check 7: Read-Only 'for' Loop Variable (assignment prohibition)
 * - Vitebsk Check 8: Forward Routine Declarations Matching
 */
public class DeclarationAndScopeChecker extends BaseASTVisitor<Void, IScope> {
    private final SemanticContext context;

    public DeclarationAndScopeChecker(SemanticContext context) {
        this.context = context;
    }

    @Override
    public Void visit(ProgramNode node, IScope ignored) {
        GlobalScope globalScope = new GlobalScope();
        context.setGlobalScope(globalScope);
        context.putNodeScope(node, globalScope);

        // Pre-pass: Register all top-level types and routine headers into GlobalScope
        for (Declaration decl : node.getDeclarations()) {
            if (decl instanceof TypeDeclarationNode t) {
                if (globalScope.findInScope(t.getName()) != null) {
                    context.addError(t.getPosition(), "Duplicate declaration of type '" + t.getName() + "'");
                } else {
                    TypeSymbol tSym = new TypeSymbol(t.getName(), t.getPosition(), t.getType());
                    globalScope.define(tSym);
                    context.putDeclaredSymbol(t, tSym);
                }
            } else if (decl instanceof RoutineDeclarationNode r) {
                Symbol existing = globalScope.findInScope(r.getName());
                if (existing != null) {
                    if (existing instanceof RoutineSymbol existingRoutine) {
                        if (existingRoutine.isForward() && !r.isForwardDeclaration()) {
                            checkSignatureMatch(existingRoutine, r);
                            context.putDeclaredSymbol(r, existingRoutine);
                        } else {
                            context.addError(r.getPosition(), "Duplicate declaration of routine '" + r.getName() + "'");
                        }
                    } else {
                        context.addError(r.getPosition(), "Identifier '" + r.getName() + "' already declared as a different symbol");
                    }
                } else {
                    List<ParameterSymbol> params = new ArrayList<>();
                    for (ParameterNode p : r.getParameters()) {
                        params.add(new ParameterSymbol(p.getName(), p.getPosition(), p.getType()));
                    }
                    RoutineSymbol rSym = new RoutineSymbol(
                            r.getName(),
                            r.getPosition(),
                            params,
                            r.getReturnType(),
                            r.isForwardDeclaration(),
                            !r.isForwardDeclaration()
                    );
                    globalScope.define(rSym);
                    context.putDeclaredSymbol(r, rSym);
                }
            }
        }

        // Vitebsk Check 2: Verify presence of mandatory entry point 'routine main()'
        Symbol mainSym = globalScope.findInScope("main");
        if (mainSym == null || !(mainSym instanceof RoutineSymbol mainRoutine)) {
            context.addError(node.getPosition(), "Mandatory entry point 'routine main() is ... end' is missing");
        } else {
            if (!mainRoutine.getParameters().isEmpty()) {
                context.addError(mainSym.getPosition(), "Entry point 'routine main' must not require parameters");
            }
        }

        // Main pass: traverse all top-level declarations
        for (Declaration decl : node.getDeclarations()) {
            decl.accept(this, globalScope);
        }

        // Vitebsk Check 8: Verify that no forward declarations remain unimplemented
        for (Symbol sym : globalScope.getSymbols().values()) {
            if (sym instanceof RoutineSymbol rSym && rSym.isForward()) {
                context.addError(rSym.getPosition(), "Forward routine '" + rSym.getName() + "' has no implementation");
            }
        }

        return null;
    }

    @Override
    public Void visit(RoutineDeclarationNode node, IScope scope) {
        // Curator Check 1: Nested routines are not supported
        if (scope != context.getGlobalScope()) {
            context.addError(node.getPosition(), "Nested routines are not supported in Language I");
            return null;
        }

        if (node.isForwardDeclaration()) {
            // Forward declarations have no body to traverse
            return null;
        }

        RoutineSymbol rSym = (RoutineSymbol) scope.findInScope(node.getName());
        if (rSym != null && rSym.isForward()) {
            rSym.setForward(false);
            rSym.setDefined(true);
        }

        RoutineScope routineScope = new RoutineScope(scope, rSym);
        context.putNodeScope(node, routineScope);

        // Define parameters in routine scope
        for (ParameterNode param : node.getParameters()) {
            if (routineScope.findInScope(param.getName()) != null) {
                context.addError(param.getPosition(), "Duplicate parameter name '" + param.getName() + "' in routine '" + node.getName() + "'");
            } else {
                ParameterSymbol paramSym = new ParameterSymbol(param.getName(), param.getPosition(), param.getType());
                routineScope.define(paramSym);
                context.putDeclaredSymbol(param, paramSym);
                param.accept(this, routineScope);
            }
        }

        if (node.getBody() != null) {
            node.getBody().accept(this, routineScope);
        }
        if (node.getExpressionBody() != null) {
            node.getExpressionBody().accept(this, routineScope);
        }

        return null;
    }

    @Override
    public Void visit(BodyNode node, IScope scope) {
        context.putNodeScope(node, scope);
        for (ASTNode item : node.getItems()) {
            item.accept(this, scope);
        }
        return null;
    }

    @Override
    public Void visit(VariableDeclarationNode node, IScope scope) {
        // Declaration before use: analyze initializer in current scope BEFORE defining the variable
        if (node.getInitializer() != null) {
            node.getInitializer().accept(this, scope);
        }
        if (node.getType() != null) {
            node.getType().accept(this, scope);
        }

        // Curator Check 2: Prohibition of duplicate variable declarations in the same scope
        if (scope.findInScope(node.getName()) != null) {
            context.addError(node.getPosition(), "Variable '" + node.getName() + "' is already declared in this scope");
        } else {
            VariableSymbol varSym = new VariableSymbol(node.getName(), node.getPosition(), node.getType(), false);
            scope.define(varSym);
            context.putDeclaredSymbol(node, varSym);
        }

        return null;
    }

    @Override
    public Void visit(TypeDeclarationNode node, IScope scope) {
        if (scope != context.getGlobalScope()) {
            if (scope.findInScope(node.getName()) != null) {
                context.addError(node.getPosition(), "Duplicate declaration of type '" + node.getName() + "'");
            } else {
                TypeSymbol tSym = new TypeSymbol(node.getName(), node.getPosition(), node.getType());
                scope.define(tSym);
                context.putDeclaredSymbol(node, tSym);
            }
        }
        if (node.getType() != null) {
            node.getType().accept(this, scope);
        }
        return null;
    }

    @Override
    public Void visit(RecordTypeNode node, IScope scope) {
        BlockScope fieldScope = new BlockScope(scope);
        context.putNodeScope(node, fieldScope);
        for (VariableDeclarationNode field : node.getFields()) {
            field.accept(this, fieldScope);
        }
        return null;
    }

    @Override
    public Void visit(VariableReferenceNode node, IScope scope) {
        // Curator Check 2: Resolution of variables before usage
        Symbol sym = scope.resolve(node.getName());
        if (sym == null) {
            context.addError(node.getPosition(), "Undeclared identifier '" + node.getName() + "'");
        } else {
            if (sym instanceof VariableSymbol varSym) {
                varSym.incrementReadCount();
            }
            context.putResolvedSymbol(node, sym);
        }
        return null;
    }

    @Override
    public Void visit(RoutineCallExpressionNode node, IScope scope) {
        // Curator Check 2: Resolution of routine calls
        Symbol sym = scope.resolve(node.getRoutineName());
        if (sym == null) {
            context.addError(node.getPosition(), "Undeclared routine '" + node.getRoutineName() + "'");
        } else if (!(sym instanceof RoutineSymbol rSym)) {
            context.addError(node.getPosition(), "'" + node.getRoutineName() + "' is not a routine");
        } else {
            context.putResolvedRoutine(node, rSym);
        }

        for (Expression arg : node.getArguments()) {
            arg.accept(this, scope);
        }
        return null;
    }

    @Override
    public Void visit(RoutineCallStatementNode node, IScope scope) {
        node.getCall().accept(this, scope);
        return null;
    }

    @Override
    public Void visit(AssignmentNode node, IScope scope) {
        // Vitebsk Check 7: Read-Only 'for' loop variable check and Target Resolution
        if (node.getTarget() instanceof VariableReferenceNode targetVar) {
            Symbol sym = scope.resolve(targetVar.getName());
            if (sym == null) {
                context.addError(targetVar.getPosition(), "Undeclared identifier '" + targetVar.getName() + "'");
            } else if (sym instanceof VariableSymbol varSym && varSym.isReadOnly()) {
                context.addError(node.getPosition(), "Cannot assign to read-only loop variable '" + targetVar.getName() + "'");
            } else {
                // Annotate the target variable without incrementing readCount (as this is a write)
                context.putResolvedSymbol(targetVar, sym);
            }
        } else if (node.getTarget() != null) {
            // Complex target (e.g. ArrayAccessNode, RecordAccessNode)
            node.getTarget().accept(this, scope);
        }

        if (node.getValue() != null) {
            node.getValue().accept(this, scope);
        }
        return null;
    }

    @Override
    public Void visit(ForLoopNode node, IScope scope) {
        // Evaluate range expressions in current scope
        node.getRange().accept(this, scope);

        // Vitebsk Check 7: Define read-only loop variable in a new BlockScope
        BlockScope forScope = new BlockScope(scope);
        VariableSymbol loopVar = new VariableSymbol(node.getVariableName(), node.getPosition(), null, true);
        forScope.define(loopVar);
        context.putDeclaredSymbol(node, loopVar);

        node.getBody().accept(this, forScope);
        return null;
    }

    @Override
    public Void visit(IfStatementNode node, IScope scope) {
        node.getCondition().accept(this, scope);

        BlockScope thenScope = new BlockScope(scope);
        node.getThenBody().accept(this, thenScope);

        if (node.getElseBody() != null) {
            BlockScope elseScope = new BlockScope(scope);
            node.getElseBody().accept(this, elseScope);
        }
        return null;
    }

    @Override
    public Void visit(WhileLoopNode node, IScope scope) {
        node.getCondition().accept(this, scope);

        BlockScope loopScope = new BlockScope(scope);
        node.getBody().accept(this, loopScope);
        return null;
    }

    @Override
    public Void visit(ReturnStatementNode node, IScope scope) {
        // Curator Check 1: Return is allowed only inside a routine body
        IScope current = scope;
        RoutineScope routineScope = null;
        while (current != null) {
            if (current instanceof RoutineScope r) {
                routineScope = r;
                break;
            }
            current = current.getEnclosingScope();
        }

        if (routineScope == null) {
            context.addError(node.getPosition(), "Return statement is not allowed outside routine body");
        }

        if (node.getValue() != null) {
            node.getValue().accept(this, scope);
        }
        return null;
    }

    @Override
    public Void visit(NamedTypeNode node, IScope scope) {
        Symbol sym = scope.resolve(node.getName());
        if (sym == null || !(sym instanceof TypeSymbol)) {
            context.addError(node.getPosition(), "Undeclared type '" + node.getName() + "'");
        }
        return null;
    }

    private void checkSignatureMatch(RoutineSymbol existing, RoutineDeclarationNode current) {
        if (existing.getParameters().size() != current.getParameters().size()) {
            context.addError(current.getPosition(), "Parameter count mismatch for forward declaration of routine '" + current.getName() + "'");
        }
    }
}

