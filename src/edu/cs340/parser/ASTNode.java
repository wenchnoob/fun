package edu.cs340.parser;

import java.util.List;
import java.util.Objects;

public class ASTNode {

    private Type type;
    private boolean markFinal;
    private Object val;
    private List<ASTNode> children;

    public ASTNode(Type type) {
        this.type = type;
    }

    public ASTNode(Type t, Object val) {
        this(t);
        this.val = val;
    }

    public ASTNode(Type t, List<ASTNode> children) {
        this(t);
        this.children = children;
    }

    public ASTNode(Type t, Object val, List<ASTNode> children) {
        this(t, val);
        this.children = children;
    }

    public Type type() {
        return this.type;
    }

    public Object val() {
        return this.val;
    }

    public List<ASTNode> children() {
        return this.children;
    }

    public void val(Object val) {
        this.val = val;
    }

    public void markFinal() {
        markFinal = true;
    }

    public boolean isFinal() {
        return markFinal;
    }

    @Override
    public String toString() {
        return String.format("ASTNode(Type=%s, Value=%s, isFinal=%s)", type, val, isFinal());
    }

    public String consolePrint() {
        switch (type) {
            case PLUS: {
                return "(" + children.get(0).consolePrint() + " + " + children.get(1).consolePrint() + ")";
            }
            case MINUS: {
                return "(" + children.get(0).consolePrint() + " - " + children.get(1).consolePrint() + ")";
            }
            case MULT: {
                return "(" + children.get(0).consolePrint() + " * " + children.get(1).consolePrint() + ")";
            }
            case DIV: {
                return "(" + children.get(0).consolePrint() + " / " + children.get(1).consolePrint() + ")";
            }
            case FLOOR_DIV: {
                return "(" + children.get(0).consolePrint() + " // " + children.get(1).consolePrint() + ")";
            }
            case MOD: {
                return "(" + children.get(0).consolePrint() + " % " + children.get(1).consolePrint() + ")";
            }
            case POW: {
                return "(" + children.get(0).consolePrint() + " ^ " + children.get(1).consolePrint() + ")";
            }
            case NEGATION: {
                return "-" + children.get(0).consolePrint();
            }
            case FACT: {
                return children.get(0).consolePrint() + "!";
            }
            case ID:
                return String.valueOf(val).split("\\*")[0];
            case NUM:
                return String.valueOf(val);
            case ASSIGN:
                return children.get(0).consolePrint();
            case FUNC_DEF:
                return children.get(0).consolePrint() + " => " + children.get(1).consolePrint();
            case APPLICATION: {
                StringBuilder sb = new StringBuilder();
                sb.append("(").append(children.get(0).consolePrint()).append(")");
                if (Objects.nonNull(children))
                    for (int i = 1; i < children.size(); i++) {
                        sb.append(children.get(i).consolePrint());
                        if (i + 1 != children.size()) sb.append(",");
                    }
                return sb.toString();
            }
            case PARAM_LIST: {
                StringBuilder sb = new StringBuilder();
                sb.append("(");
                if (Objects.nonNull(children)) for (int i = 0; i < children.size(); i++) {
                    sb.append(children.get(i).consolePrint());
                    if (i + 1 != children.size()) sb.append(",");
                }
                sb.append(")");
                return sb.toString();
            }
            default:
                return "";
        }
    }


    public enum Type {
        PLUS, MINUS, MULT, DIV, FLOOR_DIV, MOD, POW, NUM, NEGATION, FACT, ASSIGN, ID, PARAM_LIST, FUNC_DEF, APPLICATION, FUNC_BODY
    }

}
