package ru.innopolis.semantic.optimize;

import ru.innopolis.ast.ProgramNode;
import ru.innopolis.semantic.AnalysisResult;
import ru.innopolis.semantic.context.SemanticContext;

import java.util.Objects;

/**
 * Constant folding, dead-code elimination and conservative unused-local removal.
 * Constant arithmetic and conversions use the shared evaluator, not a second set of type rules.
 * Accepts the result of SemanticAnalyzer; it does not replace the validation passes.
 */
public final class OptimizerPipeline {
    private OptimizerPipeline() {
    }

    public static AnalysisResult optimize(AnalysisResult analysis) {
        Objects.requireNonNull(analysis, "Analysis result is required");
        if (!analysis.isSuccess()) return analysis;
        ProgramNode program = optimize(analysis.getProgram(), analysis.getContext());
        return AnalysisResult.success(program, analysis.getContext());
    }

    /** Reuses semantic symbols/scopes and adds annotations for replacement nodes to the context. */
    public static ProgramNode optimize(ProgramNode program, SemanticContext context) {
        Objects.requireNonNull(context, "Semantic context is required");
        ProgramNode folded = new ConstantFoldingOptimizer(context).transformProgram(program);
        ProgramNode withoutDeadCode = new DeadCodeOptimizer(context).transformProgram(folded);
        ProgramNode current = withoutDeadCode;
        while (true) {
            ProgramNode next = new UnusedVariableOptimizer(context).transformProgram(current);
            if (next == current) return current;
            // Removing an unused copy may make its source unused as well. This pass only removes
            // declarations, so repeating it terminates when no further declaration can be removed.
            current = next;
        }
    }
}
