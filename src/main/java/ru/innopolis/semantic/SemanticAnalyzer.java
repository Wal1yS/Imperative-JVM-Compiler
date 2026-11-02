package ru.innopolis.semantic;

import ru.innopolis.ast.ProgramNode;
import ru.innopolis.semantic.check.ArrayBoundsChecker;
import ru.innopolis.semantic.check.DeclarationAndScopeChecker;
import ru.innopolis.semantic.check.TypeChecker;
import ru.innopolis.semantic.context.SemanticContext;

public class SemanticAnalyzer {

    /**
     * Executes the semantic analysis pipeline.
     * Pass 1: Declaration & Scope Checking (Person 1).
     * Pass 2: Type Checking (Person 2).
     * Pass 3: Static Array Bounds Checking (Person 2).
     * Ready for the optimization passes (Person 3).
     */
    public static AnalysisResult analyze(ProgramNode ast) {
        SemanticContext context = new SemanticContext();

        // Pass 1: Declaration and Scope Resolution + Structural Checks (Person 1)
        DeclarationAndScopeChecker scopeChecker = new DeclarationAndScopeChecker(context);
        ast.accept(scopeChecker, null);

        if (context.hasErrors()) {
            return AnalysisResult.failure(context.getErrors(), context);
        }

        // Pass 2: Type checking, annotates every expression with its type (Person 2)
        TypeChecker typeChecker = new TypeChecker(context);
        ast.accept(typeChecker, context.getGlobalScope());

        if (context.hasErrors()) {
            return AnalysisResult.failure(context.getErrors(), context);
        }

        // Pass 3: Compile-time array bounds checking, uses the type annotations (Person 2)
        ArrayBoundsChecker boundsChecker = new ArrayBoundsChecker(context);
        ast.accept(boundsChecker, null);

        if (context.hasErrors()) {
            return AnalysisResult.failure(context.getErrors(), context);
        }

        // Return successfully analyzed AST and populated context
        return AnalysisResult.success(ast, context);
    }
}
