package ru.innopolis.semantic.visitor;

import ru.innopolis.ast.ASTNode;
import ru.innopolis.ast.Declaration;
import ru.innopolis.ast.ProgramNode;
import ru.innopolis.ast.declaration.*;
import ru.innopolis.ast.expression.*;
import ru.innopolis.ast.statement.*;
import ru.innopolis.ast.type.*;

public class BaseASTVisitor<R, C> implements ASTVisitor<R, C> {

    @Override
    public R visit(ProgramNode node, C context) {
        if (node.getDeclarations() != null) {
            for (Declaration decl : node.getDeclarations()) {
                decl.accept(this, context);
            }
        }
        return null;
    }

    @Override
    public R visit(RoutineDeclarationNode node, C context) {
        if (node.getParameters() != null) {
            for (ParameterNode param : node.getParameters()) {
                param.accept(this, context);
            }
        }
        if (node.getReturnType() != null) {
            node.getReturnType().accept(this, context);
        }
        if (node.getBody() != null) {
            node.getBody().accept(this, context);
        }
        if (node.getExpressionBody() != null) {
            node.getExpressionBody().accept(this, context);
        }
        return null;
    }

    @Override
    public R visit(VariableDeclarationNode node, C context) {
        if (node.getType() != null) {
            node.getType().accept(this, context);
        }
        if (node.getInitializer() != null) {
            node.getInitializer().accept(this, context);
        }
        return null;
    }

    @Override
    public R visit(TypeDeclarationNode node, C context) {
        if (node.getType() != null) {
            node.getType().accept(this, context);
        }
        return null;
    }

    @Override
    public R visit(ParameterNode node, C context) {
        if (node.getType() != null) {
            node.getType().accept(this, context);
        }
        return null;
    }

    @Override
    public R visit(BodyNode node, C context) {
        if (node.getItems() != null) {
            for (ASTNode item : node.getItems()) {
                item.accept(this, context);
            }
        }
        return null;
    }

    @Override
    public R visit(AssignmentNode node, C context) {
        if (node.getTarget() != null) {
            node.getTarget().accept(this, context);
        }
        if (node.getValue() != null) {
            node.getValue().accept(this, context);
        }
        return null;
    }

    @Override
    public R visit(IfStatementNode node, C context) {
        if (node.getCondition() != null) {
            node.getCondition().accept(this, context);
        }
        if (node.getThenBody() != null) {
            node.getThenBody().accept(this, context);
        }
        if (node.getElseBody() != null) {
            node.getElseBody().accept(this, context);
        }
        return null;
    }

    @Override
    public R visit(WhileLoopNode node, C context) {
        if (node.getCondition() != null) {
            node.getCondition().accept(this, context);
        }
        if (node.getBody() != null) {
            node.getBody().accept(this, context);
        }
        return null;
    }

    @Override
    public R visit(ForLoopNode node, C context) {
        if (node.getRange() != null) {
            node.getRange().accept(this, context);
        }
        if (node.getBody() != null) {
            node.getBody().accept(this, context);
        }
        return null;
    }

    @Override
    public R visit(RangeNode node, C context) {
        if (node.getStart() != null) {
            node.getStart().accept(this, context);
        }
        if (node.getEnd() != null) {
            node.getEnd().accept(this, context);
        }
        return null;
    }

    @Override
    public R visit(PrintStatementNode node, C context) {
        if (node.getExpressions() != null) {
            for (Expression expr : node.getExpressions()) {
                expr.accept(this, context);
            }
        }
        return null;
    }

    @Override
    public R visit(ReturnStatementNode node, C context) {
        if (node.getValue() != null) {
            node.getValue().accept(this, context);
        }
        return null;
    }

    @Override
    public R visit(RoutineCallStatementNode node, C context) {
        if (node.getCall() != null) {
            node.getCall().accept(this, context);
        }
        return null;
    }

    @Override
    public R visit(BinaryExpressionNode node, C context) {
        if (node.getLeft() != null) {
            node.getLeft().accept(this, context);
        }
        if (node.getRight() != null) {
            node.getRight().accept(this, context);
        }
        return null;
    }

    @Override
    public R visit(UnaryExpressionNode node, C context) {
        if (node.getOperand() != null) {
            node.getOperand().accept(this, context);
        }
        return null;
    }

    @Override
    public R visit(CastExpressionNode node, C context) {
        if (node.getExpression() != null) {
            node.getExpression().accept(this, context);
        }
        if (node.getTargetType() != null) {
            node.getTargetType().accept(this, context);
        }
        return null;
    }

    @Override
    public R visit(ArrayAccessNode node, C context) {
        if (node.getArray() != null) {
            node.getArray().accept(this, context);
        }
        if (node.getIndex() != null) {
            node.getIndex().accept(this, context);
        }
        return null;
    }

    @Override
    public R visit(RecordAccessNode node, C context) {
        if (node.getRecord() != null) {
            node.getRecord().accept(this, context);
        }
        return null;
    }

    @Override
    public R visit(RoutineCallExpressionNode node, C context) {
        if (node.getArguments() != null) {
            for (Expression arg : node.getArguments()) {
                arg.accept(this, context);
            }
        }
        return null;
    }

    @Override
    public R visit(VariableReferenceNode node, C context) {
        return null;
    }

    @Override
    public R visit(IntLiteralNode node, C context) {
        return null;
    }

    @Override
    public R visit(RealLiteralNode node, C context) {
        return null;
    }

    @Override
    public R visit(BoolLiteralNode node, C context) {
        return null;
    }

    @Override
    public R visit(StringLiteralNode node, C context) {
        return null;
    }

    @Override
    public R visit(PrimitiveTypeNode node, C context) {
        return null;
    }

    @Override
    public R visit(ArrayTypeNode node, C context) {
        if (node.getSize() != null) {
            node.getSize().accept(this, context);
        }
        if (node.getElementType() != null) {
            node.getElementType().accept(this, context);
        }
        return null;
    }

    @Override
    public R visit(RecordTypeNode node, C context) {
        if (node.getFields() != null) {
            for (VariableDeclarationNode field : node.getFields()) {
                field.accept(this, context);
            }
        }
        return null;
    }

    @Override
    public R visit(NamedTypeNode node, C context) {
        return null;
    }
}

