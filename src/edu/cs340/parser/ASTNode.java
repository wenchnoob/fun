package edu.cs340.parser;

import java.util.List;

public class ASTNode {

    private Type type;
    private Object val;
    private List<ASTNode> children;

    public ASTNode(Type type) {
        this.type = type;
    }

    public ASTNode (Type t, Object val) {
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

    @Override
    public String toString() {
       return String.format("ASTNode(Type=%s, Value=%s)", type, val);
    }


    public enum Type {
        PLUS, MINUS, MULT, DIV, FLOOR_DIV, MOD, POW, NUM, NEGATION, FACT, ASSIGN, ID, PARAM_LIST, FUNC_DEF, FUNC_CALL, FUNC_PARAMS, FUNC_BODY
    }

}
