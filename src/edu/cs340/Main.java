package edu.cs340;

import edu.cs340.interpreter.Interpreter;
import edu.cs340.lexer.Lexer;
import edu.cs340.lexer.Token;
import edu.cs340.parser.ASTNode;
import edu.cs340.parser.Parser;

import java.util.*;
import java.util.regex.Pattern;

public class Main {

    private static final Scanner in = new Scanner(System.in);
    private static final Pattern exit = Pattern.compile("^\\s*exit\\s*", Pattern.CASE_INSENSITIVE);
    private static final Pattern lexCommand = Pattern.compile("^\\s*lex\\s*.*\\s*", Pattern.CASE_INSENSITIVE);
    private static final Pattern parseCommand = Pattern.compile("^\\s*parse\\s*.*\\s*", Pattern.CASE_INSENSITIVE);
    private static final Pattern helpCommand = Pattern.compile("^\\s*help\\s*.*\\s*", Pattern.CASE_INSENSITIVE);

    private static final Pattern empty = Pattern.compile("^\\s*");
    private static final Pattern introCommand = Pattern.compile("^\\s*intro\\s*", Pattern.CASE_INSENSITIVE);

    private static final Pattern listVarCommand = Pattern.compile("^\\s*list vars\\s*.*\\s*", Pattern.CASE_INSENSITIVE);
    private static final Pattern dropVarCommand = Pattern.compile("^\\s*drop var\\s*.*\\s*", Pattern.CASE_INSENSITIVE);
    private static final Pattern clearVarCommand = Pattern.compile("^\\s*clear vars\\s*", Pattern.CASE_INSENSITIVE);

    public static void main(String[] args) {
        intro();
        String line;
        while (true) {
            System.out.print("=> ");
            line = in.nextLine();

            try {
                if (exit.asPredicate().test(line)) break;
                else if (lexCommand.asPredicate().test(line)) lex(line.replaceFirst("\\s*lex", ""));
                else if (parseCommand.asPredicate().test(line)) parse(line.replaceFirst("\\s*parse", ""));
                else if (helpCommand.asPredicate().test(line)) help(line.replaceFirst("\\s*help", ""));
                else if (introCommand.asPredicate().test(line)) intro();
                else if (listVarCommand.asPredicate().test(line)) listVars(line.replaceFirst("\\s*list vars\\s*", ""));
                else if (dropVarCommand.asPredicate().test(line)) dropVar(line.replaceFirst("\\s*drop var\\s*", ""));
                else if (clearVarCommand.asPredicate().test(line)) clearVars();
                else interpret(line);
            } catch (Exception e) {
                System.out.println("Something went wrong: " + e.getMessage());
            }
        }
    }

    public static void intro() {
        String line1 = "This is a calculator designed using compiler theories to ground implementation.\n\n";
        String line2 = "The calculator supports the basic operations:\n";
        String line3 = "\t+, -, /, *, ^ (exponentiation)\n\n";
        String line4 = "It also supports: \n";
        String line5 = "\t// (floor division), % (modulus)\n";
        String line6 = "\tBuilt in function: pow(x, y), sqrt(x), root(x, y), fact(x)\n\n";
        String line7 = "If you want to see the tokenization output of your expression, type in lex followed by your expression, e.g. lex {expr}\n";
        String line8 = "If you want to a AST representation of your expression, type in parse followed by your expression, e.g. parse {expr}\n";
        String line9 = "If you want to see the result of evaluation your expression, type in your expression, e.g. {expr}\n\n";
        String line10 = "If you ever need help, type in help.\n";
        System.out.printf("%s%s%s%s%s%s%s%s%s%s%n", line1, line2, line3, line4, line5, line6, line7, line8, line9, line10);
    }

    public static void prettyPrint(ASTNode ast) {
        StringBuilder sb = new StringBuilder();
        prettyPrint(ast, 0, sb);
        System.out.println(sb);
    }

    public static void inOrderPrint(ASTNode ast) {
        if (Objects.isNull(ast.children())) {
            System.out.print(ast.val() + " ");
            return;
        }

        inOrderPrint(ast.children().get(0));
        System.out.print(ast.val() + " ");
        if (ast.children().size() >= 2) inOrderPrint(ast.children().get(1));
    }

    public static void prettyPrint(ASTNode ast, int d, StringBuilder accumulator) {
        StringBuilder tabs = new StringBuilder();
        for (int i = 0; i < d; i++) tabs.append('\t');

        accumulator.append(tabs).append(ast).append('\n');

        List<ASTNode> children = ast.children();
        if (Objects.isNull(children)) return;

        for (ASTNode child : children) {
            prettyPrint(child, d + 1, accumulator);
        }
    }

    public static void help(String what) {
        if (empty.asPredicate().test(what)) {
            String line1 = "Useful commands: \n";
            String line2 = "\t\'intro\' -- to have the intro printed again\n";
            String line3 = "\t\'list vars\' -- to have all the known variables printed\n";
            String line4 = "\t\'drop {var_name}\' -- to have that function unregistered\n";
            String line5 = "\t\'clear vars\' -- to have all functions unregistered\n";
            String line6 = "\t\'help {command_name}\' -- to for a longer explanation of how a command works";
            System.out.printf("%s%s%s%s%s%s%n", line1, line2, line3, line4, line5, line6);
        }
    }

    public static void lex(String line) {
        Lexer lex = new Lexer();
        lex.init(line);

        try {
            List<Token> tokens = new ArrayList<>();
            while (lex.hasNextToken()) {
                tokens.add(lex.nextToken());
            }
            System.out.println("Tokens: ");
            for (Token t : tokens) {
                System.out.println("\t" + t);
            }
        } catch (IllegalArgumentException ex) {
            System.out.println(ex.getMessage());
        }
    }

    public static void parse(String line) {
        try {
            ASTNode ast = Parser.parse(line);
            StringBuilder accumulator = new StringBuilder();
            prettyPrint(ast, 0, accumulator);
            System.out.print(accumulator);
        } catch (IllegalArgumentException ex) {
            System.out.println(ex.getMessage());
        }
    }

    public static void interpret(String line) {
        try {
            ASTNode res = Interpreter.eval(line);
            if (Objects.nonNull(res)) {
                ASTNode oldRes = null;
                while (!res.equals(oldRes)) {
                    oldRes = res;
                    res = Interpreter.eval(res);
                }
                System.out.println(res.consolePrint());
            }
        } catch (IllegalArgumentException | IllegalStateException ex) {
            System.out.println(ex.getMessage());
        }
    }

    public static void listVars(String args) {
        Interpreter.listVars();
    }

    public static void dropVar(String var) {
        Interpreter.dropVar(var);
    }

    public static void clearVars() {
        Interpreter.clearVars();
    }

}
