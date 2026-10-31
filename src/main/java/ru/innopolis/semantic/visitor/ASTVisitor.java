package ru.innopolis.semantic.visitor;

import ru.innopolis.ast.ProgramNode;
import ru.innopolis.ast.declaration.*;
import ru.innopolis.ast.expression.*;
import ru.innopolis.ast.statement.*;
import ru.innopolis.ast.type.*;

public interface ASTVisitor<R, C> {
    R visit(ProgramNode node, C context);

    // Declarations
    R visit(RoutineDeclarationNode node, C context);
    R visit(VariableDeclarationNode node, C context);
    R visit(TypeDeclarationNode node, C context);
    R visit(ParameterNode node, C context);

    // Statements
    R visit(BodyNode node, C context);
    R visit(AssignmentNode node, C context);
    R visit(IfStatementNode node, C context);
    R visit(WhileLoopNode node, C context);
    R visit(ForLoopNode node, C context);
    R visit(RangeNode node, C context);
    R visit(PrintStatementNode node, C context);
    R visit(ReturnStatementNode node, C context);
    R visit(RoutineCallStatementNode node, C context);

    // Expressions
    R visit(BinaryExpressionNode node, C context);
    R visit(UnaryExpressionNode node, C context);
    R visit(CastExpressionNode node, C context);
    R visit(ArrayAccessNode node, C context);
    R visit(RecordAccessNode node, C context);
    R visit(RoutineCallExpressionNode node, C context);
    R visit(VariableReferenceNode node, C context);

    // Literals
    R visit(IntLiteralNode node, C context);
    R visit(RealLiteralNode node, C context);
    R visit(BoolLiteralNode node, C context);
    R visit(StringLiteralNode node, C context);

    // Types
    R visit(PrimitiveTypeNode node, C context);
    R visit(ArrayTypeNode node, C context);
    R visit(RecordTypeNode node, C context);
    R visit(NamedTypeNode node, C context);
}

