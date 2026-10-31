package ru.innopolis.semantic;

import ru.innopolis.ast.ProgramNode;
import ru.innopolis.semantic.context.SemanticContext;
import ru.innopolis.semantic.context.SemanticError;

import java.util.Collections;
import java.util.List;

public class AnalysisResult {
    private final boolean success;
    private final List<SemanticError> errors;
    private final ProgramNode program;
    private final SemanticContext context;

    private AnalysisResult(boolean success, List<SemanticError> errors, ProgramNode program, SemanticContext context) {
        this.success = success;
        this.errors = errors != null ? List.copyOf(errors) : Collections.emptyList();
        this.program = program;
        this.context = context;
    }

    public static AnalysisResult success(ProgramNode program, SemanticContext context) {
        return new AnalysisResult(true, Collections.emptyList(), program, context);
    }

    public static AnalysisResult failure(List<SemanticError> errors, SemanticContext context) {
        return new AnalysisResult(false, errors, null, context);
    }

    public boolean isSuccess() {
        return success;
    }

    public List<SemanticError> getErrors() {
        return errors;
    }

    public ProgramNode getProgram() {
        return program;
    }

    public SemanticContext getContext() {
        return context;
    }
}

