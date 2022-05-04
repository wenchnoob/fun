package edu.cs340.interpreter;

import edu.cs340.parser.ASTNode;
import edu.cs340.parser.Parser;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

public class Interpreter {

    private static Hashtable<String, ASTNode> vars = new Hashtable<>();

    static {
        loadDefaultFunctions();
    }

    private Interpreter() {
    }

    public static ASTNode eval(String src) {
        return eval(Parser.parse(src));
    }

    public static ASTNode eval(ASTNode node) {
        switch (node.type()) {
            case ASSIGN:
                return register(node);
            case ID: {
                String name = (String) node.val();
                if (vars.containsKey(name)) return vars.get(name);
                return node;
            }
            case NEGATION:
                return negate(node);
            case FACT:
                return fact(node);
            case APPLICATION:
                return evalApplication(node);
            case MULT:
            case MOD:
            case FLOOR_DIV:
            case DIV:
            case PLUS:
            case POW:
            case MINUS:
                return evalBinOp(node);
            default:
                return node;
        }
    }

    public static ASTNode evalApplication(ASTNode node) {
        if (node.type() != ASTNode.Type.APPLICATION) return eval(node);

        ASTNode lhs = node.children().get(0);

        ASTNode rhs = node.children().get(1);
        if (rhs.type() != ASTNode.Type.PARAM_LIST) {
            rhs = eval(rhs);
            node.children().set(1, rhs);
        }
        if (rhs.type() != ASTNode.Type.PARAM_LIST) return node;

        if (lhs.type() != ASTNode.Type.FUNC_DEF) {
            lhs = eval(lhs);
            node.children().set(0, lhs);
        }
        if (lhs.type() != ASTNode.Type.FUNC_DEF) return node;

        node = getVars(lhs, rhs);

        if (lhs.type() != ASTNode.Type.FUNC_DEF) return node;

        return eval(node);
    }

    private static ASTNode getVars(ASTNode lhs, ASTNode rhs) {
        List<ASTNode> formalParams = lhs.children().get(0).children();
        Hashtable<String, ASTNode> actParams = new Hashtable<>();
        for (int i = 0; i < formalParams.size() && i < rhs.children().size(); i++) {
            if (formalParams.get(i).type() == ASTNode.Type.ID) {
                actParams.put((String) formalParams.get(i).val(), rhs.children().get(i));
            }
        }

        ASTNode body = lhs.children().get(1);
        return copyWithVars(body, actParams);
    }

    private static ASTNode copyWithVars(ASTNode src, Hashtable<String, ASTNode> vars) {
        List<ASTNode> children = new LinkedList<>();

        if (Objects.nonNull(src.children())) {
            for (ASTNode child : src.children()) children.add(copyWithVars(child, vars));
        }

        if (src.type() == ASTNode.Type.ID) {
            String id = (String) src.val();
            if (vars.containsKey(id)) {
                ASTNode var = vars.get(id);
                return var;
            }
        }

        return new ASTNode(src.type(), src.val(), children);
    }

    private static ASTNode evalBinOp(ASTNode node) {
        ASTNode lhs = eval(node.children().get(0));

        if (node.type() == ASTNode.Type.MULT && lhs.type() == ASTNode.Type.NUM)
            if (lhs.val().equals(BigDecimal.ZERO)) return lhs;
            else if (lhs.val().equals(BigDecimal.ONE)) return eval(node.children().get(1));

        ASTNode rhs = eval(node.children().get(1));

        if (node.type() == ASTNode.Type.PLUS && lhs.type() == ASTNode.Type.NUM)
            if (lhs.val().equals(BigDecimal.ZERO)) return rhs;

        if (node.type() == ASTNode.Type.PLUS && rhs.type() == ASTNode.Type.NUM)
            if (rhs.val().equals(BigDecimal.ZERO)) return lhs;


        node.children().set(0, lhs);
        node.children().set(1, rhs);

        if (Objects.isNull(lhs) || lhs.type() != ASTNode.Type.NUM || Objects.isNull(rhs) || rhs.type() != ASTNode.Type.NUM) return node;

        BigDecimal l = (BigDecimal) lhs.val();
        BigDecimal r = (BigDecimal) rhs.val();

        switch (node.type()) {
            case PLUS:
                return new ASTNode(ASTNode.Type.NUM, l.add(r));
            case MINUS:
                return new ASTNode(ASTNode.Type.NUM, l.subtract(r));
            case MULT:
                return new ASTNode(ASTNode.Type.NUM, l.multiply(r));
            case DIV:
                return new ASTNode(ASTNode.Type.NUM, roundToPrecision(l.divide(r, 15, RoundingMode.HALF_UP), 15));
            case FLOOR_DIV:
                return new ASTNode(ASTNode.Type.NUM, roundToPrecision(l.divideToIntegralValue(r), 15));
            case MOD:
                return new ASTNode(ASTNode.Type.NUM, l.remainder(r));
            case POW: {
                if (r.divideToIntegralValue(BigDecimal.ONE).compareTo(r) != 0)
                    if (l.compareTo(BigDecimal.ZERO) < 0)
                        throw new IllegalStateException("Negative numbers cannot be raised to a fractional exponent");

                if (r.compareTo(BigDecimal.ZERO) < 0) {
                    r = r.negate();
                    return new ASTNode(ASTNode.Type.NUM, roundToPrecision(BigDecimal.ONE.divide(fractionallyAccuratePow(l, r), 5, RoundingMode.HALF_UP), 10));
                }

                return new ASTNode(ASTNode.Type.NUM, roundToPrecision(fractionallyAccuratePow(l, r), 15));
            }
            default:
                return eval(node);
        }
    }

    private static ASTNode fact(ASTNode node) {
        if (node.type() != ASTNode.Type.FACT) return eval(node);

        ASTNode res = eval(node.children().get(0));
        if (Objects.isNull(res) || res.type() != ASTNode.Type.NUM) {
            node.children().set(0, res);
            return node;
        }

        BigDecimal r = (BigDecimal) res.val();
        return new ASTNode(ASTNode.Type.NUM, fact(r));
    }

    private static ASTNode negate(ASTNode node) {
        if (node.type() != ASTNode.Type.NEGATION) return eval(node);

        ASTNode res = eval(node.children().get(0));
        if (Objects.isNull(res) || res.type() != ASTNode.Type.NUM) {
            node.children().set(0, res);
            return node;
        }

        BigDecimal r = (BigDecimal) res.val();
        return new ASTNode(ASTNode.Type.NUM, r.negate());
    }

    public static BigDecimal fact(BigDecimal bd) {
        BigDecimal ans = BigDecimal.ONE;
        while (!bd.equals(BigDecimal.ZERO)) {
            ans = ans.multiply(bd);
            bd = bd.compareTo(BigDecimal.ZERO) > 0 ? bd.subtract(BigDecimal.ONE) : bd.add(BigDecimal.ONE);
        }
        return ans;
    }

    public static BigDecimal fractionallyAccuratePow(BigDecimal base, BigDecimal exp) {
        BigDecimal part = exp.remainder(BigDecimal.ONE);
        int scale = 2;
        BigDecimal a = part.movePointRight(scale);
        BigDecimal b = pow(BigDecimal.valueOf(10L), BigDecimal.valueOf(scale));

        return pow(base, exp).multiply(BigDecimal.valueOf(Math.pow(base.doubleValue(), part.doubleValue())));
        // return pow(base, exp).multiply(nthRoot(b, pow(base, a)));
    }

    public static BigDecimal pow(BigDecimal base, BigDecimal exp) {
        BigDecimal ans = BigDecimal.ONE;

        BigDecimal whole = exp.divideToIntegralValue(BigDecimal.ONE);

        while (whole.compareTo(BigDecimal.ZERO) > 0) {
            ans = ans.multiply(base);
            whole = whole.subtract(BigDecimal.ONE);
        }

        return ans;
    }

    public static BigDecimal nthRoot(BigDecimal n, BigDecimal arg) {
        BigDecimal nthRoot = BigDecimal.ONE.add(BigDecimal.ONE);
        BigDecimal oldNthRoot;

        BigDecimal multiplier_a = n.subtract(BigDecimal.ONE).divide(n, 5, RoundingMode.HALF_UP);
        BigDecimal multiplier_b = arg.divide(n, 5, RoundingMode.HALF_UP);

        BigDecimal margin = BigDecimal.ONE.movePointLeft(10);
        BigDecimal delta = BigDecimal.TEN;
        BigDecimal firstTerm;
        BigDecimal secondTerm;
        BigDecimal oldTermPowNMinusOne;
        while (delta.compareTo(margin) > 0) {
            oldNthRoot = nthRoot;

            firstTerm = multiplier_a.multiply(nthRoot);

            oldTermPowNMinusOne = pow(nthRoot, n.subtract(BigDecimal.ONE));
            secondTerm = multiplier_b.multiply(BigDecimal.ONE.divide(oldTermPowNMinusOne, 16, RoundingMode.HALF_UP));

            nthRoot = firstTerm.add(secondTerm);

            delta = oldNthRoot.subtract(nthRoot);
            if (delta.compareTo(BigDecimal.ZERO) < 0) delta = delta.negate();
        }

        return nthRoot;
    }

    private static BigDecimal roundToPrecision(BigDecimal dec, int precision) {
        return dec.movePointRight(precision).divideToIntegralValue(BigDecimal.ONE).movePointLeft(precision).stripTrailingZeros();
    }

    private static ASTNode register(ASTNode node) {
        String name = (String) node.val();

        if (node.children().get(0).type() == ASTNode.Type.FUNC_DEF) {
            vars.put(name, node.children().get(0));
        } else {
            ASTNode res = eval(node.children().get(0));
            if (Objects.nonNull(res)) {
                vars.put(name, res);
                return res;
            }
        }

        return node;
    }

    public static void listVars() {
        vars.forEach((key, value) -> System.out.println(key + " = " + value.consolePrint()));
    }

    public static void clearVars() {
        Set<String> varNames = vars.keySet();
        for (String name : varNames) vars.remove(name);
        loadDefaultFunctions();
    }

    public static ASTNode dropVar(String name) {
        return vars.remove(name);
    }

    public static void loadDefaultFunctions() {
        eval("let pow = f(x, y) => x ^ y");

        eval("let root = f(x) => f(y) => y ^ (1/x)");
        eval("let sqrt = f(x) => root(2)(x)");
        eval("let cbrt = f(x) => root(3)(x)");

        /* Comparison operators */
        eval("let eq = f(x) => f(y) => isFalse(x - y)");
        eval("let neq = f(x) => f(y) => isFalse(eq(x)(y))");
        eval("let gt = f(x) => f(y) => isTrue(x-y) * eq (sqrt((x-y) ^ 2)) (x-y)");
        eval("let lt = f(x) => f(y) => isTrue(x-y) * isFalse(eq(root(2)((x-y) ^ 2))(x-y))");
        eval("let gteq = f(x, y) => isFalse(lt(x, y))");
        eval("let lteq = f(x, y) => isFalse(gt(x, y))");

        eval("let isNegative = f(x) => lt(x)(0)");
        eval("let isPositive = f(x) => gt(x)(0)");

        /* Identity */
        eval("let I = f(x) => x");

        /* Kestrel */
        eval("let K = f(x) => f(y) => x");

        /* Fusion */
        eval("let S = f(x) => f(y) => f(z) => (x(z))(y(z))");

        /* Mocking Bird */
        eval("let M = f(x) => x(x)");

        /* Swap -- Unary Postfix */
        eval("let T = f(x) => f(y) => y(x)");

        /* Function Composition */
        eval("let Z = f(x) => f(y) => f(z) => x(y(z))");

        /* Infix */
        eval("let infix = f(x) => f(y) => f(z) => (y(x))(z)");

        /* Boolean */
        eval("let isFalse = f(x) => (1- x ^ 2/(x ^ 2 + 1)) // 1");
        eval("let isTrue = f(x) => (isFalse(x) + 1) % 2");
        eval("let TRUE = f(x) => f(y) => x");
        eval("let FALSE = f(x) => f(y) => y");
        eval("let ENCODE = f(x) => isFalse(x) * FALSE + isTrue(x) * TRUE");

        /* Boolean Operators */
        eval("let NOT = f(x) => (x(FALSE))(TRUE)");
        eval("let OR = f(x) => f(y) => x(x)(y)");
        eval("let AND = f(x) => f(y) => x(y)(FALSE)");

        /* Conditionals */
        eval("let IF = f(x) => f(y) => f(z) => ENCODE(x)(y)(z)");

        /* Factorial */
        eval("let factorial = f(x) => IF (eq(x)(0)) (1) ( IF (lt(x)(0)) (tori(x + 1) * x) (tori(x-1) * x)  )");

        /* Multiplication */
        eval("let mult = f(x) => f(y) => IF (lt(x)(0)) ( 0  - mult(0 - x)(y) ) ( IF (x) ( mult(x-1)(y) + y) (0) )");

        /** Floor division **/
        eval("let fdiv = f(x) => f(y) => (x - x % y)/y");
        /* Exponentiation */
        // exp(2)(3) = 8
        // exp(3)(4) = 81
    }

}
