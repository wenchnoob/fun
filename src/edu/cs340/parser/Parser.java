package edu.cs340.parser;

import edu.cs340.lexer.Lexer;
import edu.cs340.lexer.Token;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

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
        else return additiveExpression();
    }

    private ASTNode assignment() {
        if (lookahead.type() != Token.Type.LET) fail(lookahead);
        advance();
        if (lookahead.type() != Token.Type.ID) fail(lookahead);
        String name = lookahead.value();
        advance();

        if (lookahead.type() != Token.Type.ASSIGN) fail(lookahead);
        advance();

        ASTNode rhs;
        if (lookahead.type() == Token.Type.FUNC) {
            advance();
            rhs = functionDefinition();
        }
        else rhs = additiveExpression();

        return new ASTNode(ASTNode.Type.ASSIGN, name, ls(rhs));
    }

    private ASTNode functionDefinition() {
        ASTNode params = parameterList();
        if (lookahead.type() != Token.Type.FAT_ARROW) fail(lookahead);
        advance();
        return new ASTNode(ASTNode.Type.FUNC_DEF, ls(params, additiveExpression()));
    }

    private ASTNode additiveExpression() {
        ASTNode lhs = multiplicativeExpression();

        while (lookahead.type() == Token.Type.ADDITIVE) {
            String op = lookahead.value();
            advance();

            ASTNode rhs = multiplicativeExpression();
            if (op.equals("+")) {
                lhs = new ASTNode(ASTNode.Type.PLUS, ls(lhs, rhs));
            } else {
                lhs = new ASTNode(ASTNode.Type.MINUS, ls(lhs, rhs));
            }
        }

        return lhs;
    }

    private ASTNode multiplicativeExpression() {
        ASTNode lhs = exponentExpression();

        while (lookahead.type() == Token.Type.MULTIPLICATIVE) {
            String op = lookahead.value();
            advance();

            ASTNode rhs = exponentExpression();

            switch (op) {
                case "*":
                    lhs = new ASTNode(ASTNode.Type.MULT, ls(lhs, rhs));
                    break;
                case "/":
                    lhs = new ASTNode(ASTNode.Type.DIV, ls(lhs, rhs));
                    break;
                case "//":
                    lhs = new ASTNode(ASTNode.Type.FLOOR_DIV, ls(lhs, rhs));
                    break;
                case "%":
                    lhs = new ASTNode(ASTNode.Type.MOD, ls(lhs, rhs));
                    break;
            }
        }

        return lhs;
    }

    private ASTNode exponentExpression() {
        ASTNode base = factorial();
        if (lookahead.type() != Token.Type.EXPONENT) return base;
        advance();
        return new ASTNode(ASTNode.Type.POW, ls(base, exponentExpression()));
    }

    private ASTNode factorial() {
        ASTNode lhs = primaryExpression();
        while (lookahead.type() == Token.Type.FACT) {
            advance();
            lhs = new ASTNode(ASTNode.Type.FACT, ls(lhs));
        }
        return lhs;
    }

    private ASTNode primaryExpression() {
        if (lookahead.type() == Token.Type.OPENING_PAREN) {
            advance();
            ASTNode sub = additiveExpression();
            if (lookahead.type() != Token.Type.CLOSING_PAREN) fail(lookahead);
            advance();
            return sub;
        }

        if (lookahead.type() == Token.Type.ID) {
            Token id = lookahead;
            advance();
            if (lookahead.type() == Token.Type.OPENING_PAREN) {
                advance();

                List<ASTNode> params = new ArrayList<>();
                populateParams(params);
                return new ASTNode(ASTNode.Type.FUNC_CALL, id.value(), params);
            }
            return new ASTNode(ASTNode.Type.ID, id.value());
        }

        if (lookahead.type() == Token.Type.ADDITIVE && String.valueOf(lookahead.value()).equals("-")) {
            advance();
            return new ASTNode(ASTNode.Type.NEGATION, ls(primaryExpression()));
        }

        if (lookahead.type() != Token.Type.NUMERIC) fail(lookahead);
        ASTNode pex = new ASTNode(ASTNode.Type.NUM, new BigDecimal(lookahead.value(), MathContext.DECIMAL64));
        advance();
        return pex;
    }

    private ASTNode parameterList() {
        if (lookahead.type() != Token.Type.OPENING_PAREN) fail(lookahead);
        advance();

        List<ASTNode> params = new LinkedList<>();
        populateParams(params);
        return new ASTNode(ASTNode.Type.PARAM_LIST, params);
    }

    private void populateParams(List<ASTNode> params) {
        while(lookahead.type() != Token.Type.CLOSING_PAREN) {
            ASTNode p = additiveExpression();
            params.add(p);
            if (lookahead.type() == Token.Type.CLOSING_PAREN) break;
            if (lookahead.type() != Token.Type.COMMA) fail(lookahead);
            advance();
        }

        if (lookahead.type() != Token.Type.CLOSING_PAREN) fail(lookahead);
        advance();
    }

    public void advance() {
        this.lookahead = lex.nextToken();
    }

    public void fail(Token token) {
        advance();
        throw new IllegalArgumentException("Illegal expression found: " + token.value() + " " + lookahead.value());
    }

    static <T> List<T> ls(T... elems) {
        return Arrays.asList(elems);
    }

}
