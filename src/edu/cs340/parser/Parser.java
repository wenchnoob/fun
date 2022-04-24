package edu.cs340.parser;

import edu.cs340.Main;
import edu.cs340.lexer.Lexer;
import edu.cs340.lexer.Token;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.*;

public class Parser {

    private final Lexer lex;
    private Token lookahead;

    public static ASTNode parse(String src) {
        Parser p = new Parser(src);
        ASTNode res = p.main();
        if (p.lookahead.type() != Token.Type.EOF) {
            p.fail(p.lookahead);
        }
        return res;
    }

    private Parser(String src) {
        lex = new Lexer(src);
        lookahead = lex.nextToken();
    }

    private ASTNode main() {
        if (lookahead.type() == Token.Type.LET) return assignment();
        else return functionDefinition();
    }

    /**
     * assignment ::=
     * let ID = functionDefinition |
     * functionDefinition
     */
    private ASTNode assignment() {
        if (lookahead.type() != Token.Type.LET) fail(lookahead);
        advance();
        if (lookahead.type() != Token.Type.ID) fail(lookahead);
        String name = lookahead.value();
        advance();

        if (lookahead.type() != Token.Type.ASSIGN) fail(lookahead);
        advance();

        ASTNode rhs = functionDefinition();
        return new ASTNode(ASTNode.Type.ASSIGN, name, ls(rhs));
    }

    /**
     * functionDefinition ::=
     * f ( parameterList ) => functionDefinition |
     * additiveExpression
     */
    private ASTNode functionDefinition() {
        if (lookahead.type() != Token.Type.FUNC) return additiveExpression();
        advance();
        if (lookahead.type() != Token.Type.OPENING_PAREN) fail(lookahead);
        advance();
        ASTNode params = parameterList();
        if (lookahead.type() != Token.Type.CLOSING_PAREN) fail(lookahead);
        advance();
        if (lookahead.type() != Token.Type.FAT_ARROW) fail(lookahead);
        advance();
        ASTNode fnDef = functionDefinition();

        Hashtable<String, String> mappings = new Hashtable<>();
        for (ASTNode n: params.children()) {
            if (n.type() != ASTNode.Type.ID) continue;
            String id = (String) n.val();
            String newId = id + "*" + UUID.randomUUID();
            mappings.put(id, newId);
            n.val(newId);
            n.markFinal();
        }

        renameLocals(fnDef, mappings);

        return new ASTNode(ASTNode.Type.FUNC_DEF, ls(params, fnDef));
    }

    private void renameLocals(ASTNode node, Hashtable<String, String> mappings) {
        if (Objects.isNull(node)) return;
        if (Objects.nonNull(node.children())) {
            for (ASTNode c: node.children()) renameLocals(c, mappings);
        }

        if (node.type() != ASTNode.Type.ID) return;
        if (node.isFinal()) return;

        String curName = (String)node.val();
        if (mappings.containsKey(curName)) {
            node.val(mappings.get(curName));
            node.markFinal();
        }
    }

    /**
     * additiveExpression ::=
     * multiplicativeExpression + additiveExpression |
     * multiplicativeExpression - additiveExpression |
     * multiplicativeExpression
     */
    private ASTNode additiveExpression() {
        ASTNode lhs = multiplicativeExpression();

        while (lookahead.type() == Token.Type.ADDITIVE) {
            String op = lookahead.value();
            advance();

            ASTNode rhs = multiplicativeExpression();
            if (op.equals("+")) {
                lhs = new ASTNode(ASTNode.Type.PLUS, op, ls(lhs, rhs));
            } else {
                lhs = new ASTNode(ASTNode.Type.MINUS, op, ls(lhs, rhs));
            }
        }

        return lhs;
    }

    /**
     * multiplicativeExpression ::=
     * exponentExpression * multiplicativeExpression |
     * exponentExpression / multiplicativeExpression |
     * exponentExpression // multiplicativeExpression |
     * exponentExpression % multiplicativeExpression |
     * exponentExpression
     */
    private ASTNode multiplicativeExpression() {
        ASTNode lhs = exponentExpression();

        while (lookahead.type() == Token.Type.MULTIPLICATIVE) {
            String op = lookahead.value();
            advance();

            ASTNode rhs = exponentExpression();

            switch (op) {
                case "*":
                    lhs = new ASTNode(ASTNode.Type.MULT, op, ls(lhs, rhs));
                    break;
                case "/":
                    lhs = new ASTNode(ASTNode.Type.DIV, op, ls(lhs, rhs));
                    break;
                case "//":
                    lhs = new ASTNode(ASTNode.Type.FLOOR_DIV, op, ls(lhs, rhs));
                    break;
                case "%":
                    lhs = new ASTNode(ASTNode.Type.MOD, op, ls(lhs, rhs));
                    break;
            }
        }

        return lhs;
    }

    /**
     * exponentExpression ::=
     * factorial ^ exponentExpression |
     * factorial
     **/
    private ASTNode exponentExpression() {
        ASTNode base = factorial();
        if (lookahead.type() != Token.Type.EXPONENT) return base;
        advance();
        return new ASTNode(ASTNode.Type.POW, "^", ls(base, exponentExpression()));
    }

    /**
     * factorial ::=
     * factorial ! |
     * application
     */
    private ASTNode factorial() {
        ASTNode lhs = application();
        while (lookahead.type() == Token.Type.FACT) {
            advance();
            lhs = new ASTNode(ASTNode.Type.FACT, "!", ls(lhs));
        }
        return lhs;
    }

    /**
     * application ::=
     * application paramList |
     * primaryExpression
     */
    private ASTNode application() {
        ASTNode lhs = primaryExpression();

        while (lookahead.type() == Token.Type.OPENING_PAREN) {
            advance();
            lhs = new ASTNode(ASTNode.Type.APPLICATION, ls(lhs, parameterList()));
            if (lookahead.type() != Token.Type.CLOSING_PAREN) fail(lookahead);
            advance();
        }

        return lhs;
    }

    /**
     * primaryExpression ::=
     * ( functionDefinition ) |
     * - primaryExpression |
     * ID |
     * NUMBER
     */
    private ASTNode primaryExpression() {
        if (lookahead.type() == Token.Type.OPENING_PAREN) {
            advance();
            ASTNode sub = functionDefinition();
            if (lookahead.type() != Token.Type.CLOSING_PAREN) fail(lookahead);
            advance();
            return sub;
        }

        if (lookahead.type() == Token.Type.ADDITIVE && String.valueOf(lookahead.value()).equals("-")) {
            advance();
            return new ASTNode(ASTNode.Type.NEGATION, "-", ls(primaryExpression()));
        }

        if (lookahead.type() == Token.Type.ID) {
            Token la = lookahead;
            advance();

            return new ASTNode(ASTNode.Type.ID, la.value());
        }

        if (lookahead.type() != Token.Type.NUMERIC) fail(lookahead);
        ASTNode pex = new ASTNode(ASTNode.Type.NUM, new BigDecimal(lookahead.value(), MathContext.DECIMAL64));
        advance();
        return pex;
    }

    /**
     * parameterList ::=
     * neParameterList |
     * { empty }
     * neParameterList ::=
     * functionDefinition, parameterList |
     * functionDefinition
     */
    private ASTNode parameterList() {
        List<ASTNode> params = new LinkedList<>();
        while (lookahead.type() != Token.Type.CLOSING_PAREN) {
            params.add(functionDefinition());
            if (lookahead.type() == Token.Type.CLOSING_PAREN) break;
            if (lookahead.type() != Token.Type.COMMA) fail(lookahead);
            advance();
        }
        return new ASTNode(ASTNode.Type.PARAM_LIST, params);
    }

    public void advance() {
        this.lookahead = lex.nextToken();
    }

    public void fail(Token token) {
        advance();
        throw new IllegalArgumentException("Illegal expression found: " + token.value() + " " + lookahead.value());
    }

    public static <T> List<T> ls(T... elems) {
        List<T> linkedList = new LinkedList<>();
        for(T t: elems) linkedList.add(t);
        return linkedList;
    }

}
