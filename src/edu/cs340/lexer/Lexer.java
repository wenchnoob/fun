package edu.cs340.lexer;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static edu.cs340.lexer.Token.Type;

public class Lexer {

    private static final Spec[] specs = new Spec[]{
            new Spec(Pattern.compile("^\\s+"), null),
            new Spec(Pattern.compile("^\\("), Type.OPENING_PAREN),
            new Spec(Pattern.compile("^\\)"), Type.CLOSING_PAREN),
            new Spec(Pattern.compile("^,"), Type.COMMA),
            new Spec(Pattern.compile("^\\^"), Type.EXPONENT),
            new Spec(Pattern.compile("^(\\*|//|/|%)"), Type.MULTIPLICATIVE),
            new Spec(Pattern.compile("^(\\+|-)"), Type.ADDITIVE),
            new Spec(Pattern.compile("^!"), Type.FACT),
            new Spec(Pattern.compile("^=>"), Type.FAT_ARROW),
            new Spec(Pattern.compile("^="), Type.ASSIGN),
            new Spec(Pattern.compile("^(\\d+(\\d+|\\.\\d+)?|\\.\\d+)"), Type.NUMERIC),
            new Spec(Pattern.compile("^let"), Type.LET),
            new Spec(Pattern.compile("^f"), Type.FUNC),
            new Spec(Pattern.compile("^[a-zA-Z_]\\w*"), Type.ID)
    };

    private String src;
    private int cursor = 0;

    public Lexer() {
    }

    public Lexer(String src) {
        this.src = src;
    }

    public void init(String src) {
        this.src = src;
        cursor = 0;
    }

    public boolean hasNextToken() {
        return cursor < src.length();
    }

    public Token nextToken() {
        if (cursor == src.length()) return new Token("", Type.EOF);

        String match = null;
        Spec matchedSpec = null;
        for (Spec spec : specs) {
            String curMatch = match(spec.pat, src);
            if (Objects.isNull(curMatch)) continue;
            if (Objects.isNull(match) || curMatch.length() > match.length()) {
                match = curMatch;
                matchedSpec = spec;
            }
        }

        if (Objects.nonNull(match)) {
            cursor += match.length();
            if (Objects.isNull(matchedSpec.t)) return nextToken();
            return new Token(match, matchedSpec.t);
        }
        throw new IllegalArgumentException("Illegal character found in input: " + src.charAt(cursor));
    }

    public String match(Pattern pat, String targ) {
        Matcher m = pat.matcher(targ.substring(cursor));
        if (m.find()) {
            String match = m.group();
            return match;
        }
        return null;
    }

    private static class Spec {
        Pattern pat;
        Type t;

        public Spec(Pattern pat, Type t) {
            this.pat = pat;
            this.t = t;
        }
    }
}
