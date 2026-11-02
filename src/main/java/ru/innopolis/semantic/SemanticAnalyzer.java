package ru.innopolis.semantic;

import ru.innopolis.ast.ProgramNode;
import ru.innopolis.semantic.check.ArrayBoundsChecker;
import ru.innopolis.semantic.check.DeclarationAndScopeChecker;
import ru.innopolis.semantic.check.TypeChecker;
import ru.innopolis.semantic.context.SemanticContext;
import ru.innopolis.semantic.optimize.OptimizerPipeline;

public class SemanticAnalyzer {

    /**
     * Executes the semantic analysis pipeline.
     * Pass 1: Declarations and scopes (Person 1).
     * Passes 2 and 3: Types and static array bounds (Person 2).
     * Pass 4: AST optimizations (Person 3), only after all checks pass.
     */
    public static AnalysisResult analyze(ProgramNode ast) {
        SemanticContext context = new SemanticContext();

        // Pass 1: Declaration and Scope Resolution + Structural Checks (Person 1)
        DeclarationAndScopeChecker scopeChecker = new DeclarationAndScopeChecker(context);
        ast.accept(scopeChecker, null);

        if (context.hasErrors()) {
            return AnalysisResult.failure(context.getErrors(), context);
        }

        TypeChecker typeChecker = new TypeChecker(context);
        ast.accept(typeChecker, context.getGlobalScope());

        if (context.hasErrors()) {
            return AnalysisResult.failure(context.getErrors(), context);
        }

        ArrayBoundsChecker boundsChecker = new ArrayBoundsChecker(context);
        ast.accept(boundsChecker, null);

        if (context.hasErrors()) {
            return AnalysisResult.failure(context.getErrors(), context);
        }

        // Optimize only the successfully checked tree, retaining its semantic annotations.
        return OptimizerPipeline.optimize(AnalysisResult.success(ast, context));
    }
}
