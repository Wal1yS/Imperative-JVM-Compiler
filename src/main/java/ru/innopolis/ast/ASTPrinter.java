package ru.innopolis.ast;

import ru.innopolis.ast.declaration.*;
import ru.innopolis.ast.expression.*;
import ru.innopolis.ast.statement.*;
import ru.innopolis.ast.type.*;

import java.util.ArrayList;
import java.util.List;

public class ASTPrinter {

    public static String print(ASTNode root) {
        if (root == null) return "<empty>";
        StringBuilder sb = new StringBuilder();
        printTree(root, "", true, sb, true);
        return sb.toString();
    }

    private static void printTree(ASTNode node, String prefix, boolean isTail, StringBuilder sb, boolean isRoot) {
        if (node == null) return;

        if (isRoot) {
            sb.append(formatLabel(node)).append("\n");
        } else {
            sb.append(prefix).append(isTail ? "\\-- " : "|-- ").append(formatLabel(node)).append("\n");
        }

        String nextPrefix = isRoot ? "" : prefix + (isTail ? "    " : "|   ");
        List<ASTNode> children = getChildren(node);

        for (int i = 0; i < children.size(); i++) {
            boolean last = (i == children.size() - 1);
            printTree(children.get(i), nextPrefix, last, sb, false);
        }
    }

    private static String formatLabel(ASTNode node) {
        if (node instanceof ProgramNode) {
            return "ProgramNode";
        }
        if (node instanceof RoutineDeclarationNode r) {
            String ret = r.getReturnType() != null ? " : " + typeToString(r.getReturnType()) : "";
            String forward = r.isForwardDeclaration() ? " [forward]" : "";
            return "RoutineDeclaration: " + r.getName() + ret + forward;
        }
        if (node instanceof ParameterNode p) {
            return "Parameter: " + p.getName() + " : " + typeToString(p.getType());
        }
        if (node instanceof VariableDeclarationNode v) {
            String type = v.getType() != null ? " : " + typeToString(v.getType()) : "";
            return "VariableDeclaration: " + v.getName() + type;
        }
        if (node instanceof TypeDeclarationNode t) {
            return "TypeDeclaration: " + t.getName() + " is " + typeToString(t.getType());
        }
        if (node instanceof BodyNode) {
            return "Body";
        }
        if (node instanceof AssignmentNode) {
            return "Assignment";
        }
        if (node instanceof RoutineCallStatementNode) {
            return "RoutineCallStatement";
        }
        if (node instanceof IfStatementNode) {
            return "IfStatement";
        }
        if (node instanceof WhileLoopNode) {
            return "WhileLoop";
        }
        if (node instanceof ForLoopNode f) {
            return "ForLoop: " + f.getVariableName() + (f.isReverse() ? " [reverse]" : "");
        }
        if (node instanceof RangeNode r) {
            return r.isArrayIteration() ? "Range (array iteration)" : "Range";
        }
        if (node instanceof PrintStatementNode) {
            return "PrintStatement";
        }
        if (node instanceof ReturnStatementNode r) {
            return r.getValue() == null ? "ReturnStatement (void)" : "ReturnStatement";
        }
        if (node instanceof BinaryExpressionNode b) {
            return "BinaryExpression: " + b.getOperator();
        }
        if (node instanceof UnaryExpressionNode u) {
            return "UnaryExpression: " + u.getOperator();
        }
        if (node instanceof CastExpressionNode c) {
            return "CastExpression: as " + typeToString(c.getTargetType());
        }
        if (node instanceof ArrayAccessNode) {
            return "ArrayAccess";
        }
        if (node instanceof RecordAccessNode rec) {
            return "RecordAccess: ." + rec.getFieldName();
        }
        if (node instanceof RoutineCallExpressionNode rc) {
            return "RoutineCall: " + rc.getRoutineName();
        }
        if (node instanceof VariableReferenceNode v) {
            return "VariableReference: " + v.getName();
        }
        if (node instanceof IntLiteralNode i) {
            return "IntLiteral: " + i.getValue();
        }
        if (node instanceof RealLiteralNode r) {
            return "RealLiteral: " + r.getValue();
        }
        if (node instanceof BoolLiteralNode b) {
            return "BoolLiteral: " + b.getValue();
        }
        if (node instanceof StringLiteralNode s) {
            return "StringLiteral: \"" + s.getValue() + "\"";
        }
        if (node instanceof PrimitiveTypeNode p) {
            return "PrimitiveType: " + p.getKind().name().toLowerCase();
        }
        if (node instanceof NamedTypeNode n) {
            return "NamedType: " + n.getName();
        }
        if (node instanceof ArrayTypeNode) {
            return "ArrayType";
        }
        if (node instanceof RecordTypeNode) {
            return "RecordType";
        }
        return node.getClass().getSimpleName();
    }

    private static List<ASTNode> getChildren(ASTNode node) {
        List<ASTNode> children = new ArrayList<>();
        if (node instanceof ProgramNode p) {
            children.addAll(p.getDeclarations());
        } else if (node instanceof RoutineDeclarationNode r) {
            children.addAll(r.getParameters());
            if (r.getBody() != null) children.add(r.getBody());
            if (r.getExpressionBody() != null) children.add(r.getExpressionBody());
        } else if (node instanceof VariableDeclarationNode v) {
            if (v.getInitializer() != null) children.add(v.getInitializer());
        } else if (node instanceof BodyNode b) {
            children.addAll(b.getItems());
        } else if (node instanceof AssignmentNode a) {
            children.add(a.getTarget());
            children.add(a.getValue());
        } else if (node instanceof RoutineCallStatementNode rc) {
            children.add(rc.getCall());
        } else if (node instanceof IfStatementNode ifs) {
            children.add(ifs.getCondition());
            children.add(ifs.getThenBody());
            if (ifs.getElseBody() != null) children.add(ifs.getElseBody());
        } else if (node instanceof WhileLoopNode w) {
            children.add(w.getCondition());
            children.add(w.getBody());
        } else if (node instanceof ForLoopNode f) {
            children.add(f.getRange());
            children.add(f.getBody());
        } else if (node instanceof RangeNode rng) {
            children.add(rng.getStart());
            if (rng.getEnd() != null) children.add(rng.getEnd());
        } else if (node instanceof PrintStatementNode p) {
            children.addAll(p.getExpressions());
        } else if (node instanceof ReturnStatementNode ret) {
            if (ret.getValue() != null) children.add(ret.getValue());
        } else if (node instanceof BinaryExpressionNode b) {
            children.add(b.getLeft());
            children.add(b.getRight());
        } else if (node instanceof UnaryExpressionNode u) {
            children.add(u.getOperand());
        } else if (node instanceof CastExpressionNode c) {
            children.add(c.getExpression());
        } else if (node instanceof ArrayAccessNode a) {
            children.add(a.getArray());
            children.add(a.getIndex());
        } else if (node instanceof RecordAccessNode rec) {
            children.add(rec.getRecord());
        } else if (node instanceof RoutineCallExpressionNode call) {
            children.addAll(call.getArguments());
        } else if (node instanceof ArrayTypeNode arr) {
            if (arr.getSize() != null) children.add(arr.getSize());
            children.add(arr.getElementType());
        } else if (node instanceof RecordTypeNode rec) {
            children.addAll(rec.getFields());
        }
        return children;
    }

    public static String typeToString(TypeNode type) {
        if (type == null) return "void";
        if (type instanceof PrimitiveTypeNode p) {
            return p.getKind().name().toLowerCase();
        }
        if (type instanceof NamedTypeNode n) {
            return n.getName();
        }
        if (type instanceof ArrayTypeNode a) {
            String sizeStr = a.getSize() != null ? " [" + exprToString(a.getSize()) + "] " : " ";
            return "array" + sizeStr + typeToString(a.getElementType());
        }
        if (type instanceof RecordTypeNode) {
            return "record";
        }
        return type.getClass().getSimpleName();
    }

    public static String exprToString(Expression expr) {
        if (expr == null) return "";
        if (expr instanceof IntLiteralNode i) return String.valueOf(i.getValue());
        if (expr instanceof RealLiteralNode r) return String.valueOf(r.getValue());
        if (expr instanceof BoolLiteralNode b) return String.valueOf(b.getValue());
        if (expr instanceof StringLiteralNode s) return "\"" + s.getValue() + "\"";
        if (expr instanceof VariableReferenceNode v) return v.getName();
        if (expr instanceof BinaryExpressionNode b) {
            return exprToString(b.getLeft()) + " " + b.getOperator().name().toLowerCase() + " " + exprToString(b.getRight());
        }
        if (expr instanceof UnaryExpressionNode u) {
            return u.getOperator().name().toLowerCase() + exprToString(u.getOperand());
        }
        if (expr instanceof CastExpressionNode c) {
            return exprToString(c.getExpression()) + " as " + typeToString(c.getTargetType());
        }
        if (expr instanceof ArrayAccessNode a) {
            return exprToString(a.getArray()) + "[" + exprToString(a.getIndex()) + "]";
        }
        if (expr instanceof RecordAccessNode rec) {
            return exprToString(rec.getRecord()) + "." + rec.getFieldName();
        }
        if (expr instanceof RoutineCallExpressionNode rc) {
            return rc.getRoutineName() + "(...)";
        }
        return "...";
    }
}
