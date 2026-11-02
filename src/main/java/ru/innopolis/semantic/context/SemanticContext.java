package ru.innopolis.semantic.context;

import ru.innopolis.ast.ASTNode;
import ru.innopolis.ast.expression.Expression;
import ru.innopolis.ast.expression.RoutineCallExpressionNode;
import ru.innopolis.ast.expression.VariableReferenceNode;
import ru.innopolis.lexer.Span;
import ru.innopolis.semantic.scope.GlobalScope;
import ru.innopolis.semantic.scope.IScope;
import ru.innopolis.semantic.symbol.RoutineSymbol;
import ru.innopolis.semantic.symbol.Symbol;
import ru.innopolis.semantic.type.Type;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SemanticContext {
    private final List<SemanticError> errors = new ArrayList<>();
    private final Map<ASTNode, IScope> nodeScopes = new HashMap<>();
    private final Map<VariableReferenceNode, Symbol> resolvedSymbols = new HashMap<>();
    private final Map<RoutineCallExpressionNode, RoutineSymbol> resolvedRoutines = new HashMap<>();
    private final Map<ASTNode, Symbol> declaredSymbols = new HashMap<>();
    private final Map<Expression, Type> expressionTypes = new HashMap<>();
    private final Map<Symbol, Type> symbolTypes = new HashMap<>();
    private GlobalScope globalScope;

    public void addError(Span position, String message) {
        errors.add(new SemanticError(position, message));
    }

    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    public List<SemanticError> getErrors() {
        return errors;
    }

    public GlobalScope getGlobalScope() {
        return globalScope;
    }

    public void setGlobalScope(GlobalScope globalScope) {
        this.globalScope = globalScope;
    }

    public void putNodeScope(ASTNode node, IScope scope) {
        nodeScopes.put(node, scope);
    }

    public IScope getNodeScope(ASTNode node) {
        return nodeScopes.get(node);
    }

    public void putResolvedSymbol(VariableReferenceNode node, Symbol symbol) {
        resolvedSymbols.put(node, symbol);
    }

    public Symbol getResolvedSymbol(VariableReferenceNode node) {
        return resolvedSymbols.get(node);
    }

    public void putResolvedRoutine(RoutineCallExpressionNode node, RoutineSymbol routine) {
        resolvedRoutines.put(node, routine);
    }

    public RoutineSymbol getResolvedRoutine(RoutineCallExpressionNode node) {
        return resolvedRoutines.get(node);
    }

    public void putDeclaredSymbol(ASTNode node, Symbol symbol) {
        declaredSymbols.put(node, symbol);
    }

    public Symbol getDeclaredSymbol(ASTNode node) {
        return declaredSymbols.get(node);
    }

    public void putExpressionType(Expression node, Type type) {
        expressionTypes.put(node, type);
    }

    public Type getExpressionType(Expression node) {
        return expressionTypes.get(node);
    }

    public void putSymbolType(Symbol symbol, Type type) {
        symbolTypes.put(symbol, type);
    }

    public Type getSymbolType(Symbol symbol) {
        return symbolTypes.get(symbol);
    }
}

