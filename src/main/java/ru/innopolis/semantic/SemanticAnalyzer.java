package ru.innopolis.semantic;

import ru.innopolis.ast.ProgramNode;
import ru.innopolis.semantic.check.ArrayBoundsChecker;
import ru.innopolis.semantic.check.DeclarationAndScopeChecker;
import ru.innopolis.semantic.check.TypeChecker;
import ru.innopolis.semantic.context.SemanticContext;

public class SemanticAnalyzer {

    /**
     * Executes the semantic analysis pipeline.
     * Currently runs Pass 1: Declaration & Scope Checking (Person 1).
     * Ready for Pass 2 (Person 2: TypeChecker) and Pass 3 (Person 3: Optimizers).
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

        // Return successfully analyzed AST and populated context
        return AnalysisResult.success(ast, context);
    }
}
