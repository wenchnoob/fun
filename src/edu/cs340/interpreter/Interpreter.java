package edu.cs340.interpreter;

import edu.cs340.Main;
import edu.cs340.parser.ASTNode;
import edu.cs340.parser.Parser;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

public class Interpreter {

    private static Hashtable<String, BigDecimal> vars = new Hashtable<>();
    private static Hashtable<String, Hashtable<Integer, ASTNode>> funcs = new Hashtable<>();

    static {
        loadDefaultFunctions();
    }

    private Interpreter() {
    }

    public static ASTNode eval(String src) {
        return eval(Parser.parse(src));
    }

    public static ASTNode eval(ASTNode node) {
        if (Objects.isNull(node)) return null;

        if (node.type() == ASTNode.Type.ASSIGN) {
            register(node);
            return null;
        }
        return evalOp(node);
    }

    public static ASTNode evalOp(ASTNode node) {
        if (node.type() == ASTNode.Type.NUM) return node;

        switch (node.type()) {
            case NUM:
                return node;
            case ID: {
                String name = (String)node.val();
                if (vars.containsKey(name)) return new ASTNode(ASTNode.Type.NUM, vars.get(name));
                throw new IllegalStateException("The variable { " + name + " } does not exist.");
            }
            case NEGATION: return negate(node);
            case FACT: return fact(node);
            case FUNC_CALL: return evalFunctionCall(node);
            default: return evalBinOp(node);
        }
    }

    public static ASTNode evalFunctionCall(ASTNode node) {
        if (node.type() != ASTNode.Type.FUNC_CALL) return eval(node);
        Hashtable<Integer, ASTNode> overloads = funcs.get((String) node.val());
        if (Objects.isNull(overloads)) throw new IllegalStateException("The function { " + node.val() + " } does not exist.");
        ASTNode func = overloads.get(node.children().size());
        if (Objects.isNull(func)) throw new IllegalStateException("The function { " + node.val() + " } takes these number of arguments: " + overloads.keySet());


        Hashtable<String, ASTNode> actParams = new Hashtable<>();
        List<ASTNode> formalParams = func.children().get(0).children().get(0).children();
        for (int i = 0; i < formalParams.size(); i++) {
            actParams.put((String)formalParams.get(i).val(), node.children().get(i));
        }

        ASTNode substitution = copyWithVars(func.children().get(0).children().get(1), actParams);

        return eval(substitution);
    }

    public static ASTNode evalBinOp(ASTNode node) {
        ASTNode lhs = eval(node.children().get(0));
        ASTNode rhs = eval(node.children().get(1));

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
                return new ASTNode(ASTNode.Type.NUM, l.divide(r, 5, RoundingMode.HALF_UP).stripTrailingZeros());
            case FLOOR_DIV:
                return new ASTNode(ASTNode.Type.NUM, l.divideToIntegralValue(r));
            case MOD:
                return new ASTNode(ASTNode.Type.NUM, l.remainder(r));
            case POW: {
                return new ASTNode(ASTNode.Type.NUM, BigDecimal.valueOf(Math.pow(l.doubleValue(), r.doubleValue())).stripTrailingZeros());
            }
            default: return eval(node);
        }


//        if (r.intValue() >= 0) return new ASTNode(ASTNode.Type.NUM, l.(r.doubleValue()));
//                else return new ASTNode(ASTNode.Type.NUM, BigDecimal.ONE.divide(l.pow(-r.intValue()), 5, RoundingMode.HALF_UP).stripTrailingZeros());
    }

    public static ASTNode fact(ASTNode node) {
        if (node.type() != ASTNode.Type.FACT) return eval(node);

        ASTNode res = eval(node.children().get(0));
        if (Objects.isNull(res)) throw new IllegalStateException("Bad call to factorial");

        BigDecimal r = (BigDecimal) res.val();
        return new ASTNode(ASTNode.Type.NUM, fact(r));
    }

    public static ASTNode negate(ASTNode node) {
        if (node.type() != ASTNode.Type.NEGATION) return eval(node);

        ASTNode res = eval(node.children().get(0));
        if (Objects.isNull(res)) throw new IllegalStateException("Bad call to factorial");

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

    public static void register(ASTNode node) {
        String name = (String) node.val();

        if (node.children().get(0).type() == ASTNode.Type.FUNC_DEF) {
            int argLengths = node.children().get(0).children().get(0).children().size();

            Hashtable<Integer, ASTNode> overloads = funcs.getOrDefault(name, new Hashtable<>());
            overloads.put(argLengths, node);
            funcs.put(name, overloads);
        } else {
            ASTNode res = eval(node.children().get(0));
            if (Objects.nonNull(res))
                if (res.val() instanceof BigDecimal) {
                    vars.put(name, (BigDecimal) res.val());
                    return;
                }
                throw new IllegalStateException("Failed to register val: " + name);
        }
    }

    public static ASTNode copyWithVars(ASTNode src, Hashtable<String, ASTNode> vars) {
        if (src.type() == ASTNode.Type.ID) {
            String id = (String) src.val();
            if (vars.containsKey(id)) return vars.get(id);
            return eval(src);
        }

        List<ASTNode> children = new ArrayList<>();
        if (Objects.nonNull(src.children())) {
            for (ASTNode child: src.children()) children.add(copyWithVars(child, vars));
        }

        return new ASTNode(src.type(), src.val(), children);
    }

    public static void listFuncs() {
        Set<String> fnames = funcs.keySet();
        for (String name : fnames) {
            Set<Integer> overloads = funcs.get(name).keySet();
            for (int overload : overloads) {
                System.out.println(name + "::" + overload);
                StringBuilder toPrint = new StringBuilder();
                Main.prettyPrint(funcs.get(name).get(overload), 0, toPrint);
                System.out.println(toPrint);
            }
        }
    }

    public static void inspectFunc(String name, int overload) {

    }

    public static void clearFuncs() {
        Set<String> funcNames = funcs.keySet();
        for (String name: funcNames) funcs.remove(name);
        loadDefaultFunctions();
    }

    public static void dropFunc(String name, int overload) {
        if (!funcs.containsKey(name)) return;
        if (overload == -1) funcs.remove(name);
        else funcs.get(name).remove(overload);
    }

    public static void listVars() {
        Set<String> valNames = vars.keySet();
        for (String name : valNames) {
            BigDecimal bd = vars.get(name);
            if (Objects.isNull(bd)) throw new IllegalStateException("Variable is not recognized.");
            System.out.println(name + " = " + vars.get(name).toEngineeringString());
        }
    }

    public static void dropVar(String name) {
        vars.remove(name);
    }

    public static void clearVars() {
        Set<String> valNames = vars.keySet();
        for (String name: valNames) vars.remove(name);
    }

    public static void loadDefaultFunctions() {
        ASTNode pow = Parser.parse("let pow = f(x, y) => x ^ y");
        ASTNode root = Parser.parse("let root = f(x, y) => y ^ (1/x)");
        ASTNode fact = Parser.parse("let fact = f(x) => x!");

        eval(pow);
        eval(root);
        eval(fact);
    }

}
