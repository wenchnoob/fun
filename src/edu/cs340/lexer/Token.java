package edu.cs340.lexer;

public class Token {

    private String value;
    private Type type;

    public Token(String value, Type type) {
        this.value = value;
        this.type = type;
    }

    public String value() {
        return this.value;
    }

    public Type type() {
        return type;
    }

    @Override
    public String toString() {
        return String.format("Token(value=%s, type=%s)", value, type);
    }

    public enum Type {
        FACT, EXPONENT, MULTIPLICATIVE, ADDITIVE, NUMERIC, OPENING_PAREN, CLOSING_PAREN, EOF, ID, COMMA, LET, ASSIGN, FUNC, FAT_ARROW
    }
}