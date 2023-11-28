package prolog;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Lexer {

    private static final String validNumberRegexp = "^-?(?:0|[1-9]\\d*)(?:\\.\\d+)?(?:[eE][+-]?\\d+)?$";

    private final Reader reader;
    private final LinkedList<Character> nextCharQueue;
    private final Pattern validNumberPattern;

    public Lexer(String string) {
        this(new StringReader(string));
    }

    public Lexer(Reader reader) {
        this.reader = reader;
        this.nextCharQueue = new LinkedList<>();
        this.validNumberPattern = Pattern.compile(validNumberRegexp);
    }

    public Tokens tokens() throws IOException {
        var tokens = new ArrayList<TokenValue>();
        var token = this.next();
        while (token != null && token.token != Token.EOF) {
            tokens.add(token);
            token = this.next();
        }
        return new Tokens(tokens.toArray(TokenValue[]::new));
    }

    public TokenValue next() throws IOException {
        return this.parseNextToken();
    }

    private Character readNext() throws IOException {
        if (this.nextCharQueue.isEmpty()) {
            var ch = this.reader.read();
            if (ch < 0) return null;
            return (char)ch;
        } else {
            return this.nextCharQueue.poll();
        }
    }
    private Character peekNext() throws IOException {
        if (this.nextCharQueue.isEmpty()) {
            var ch = this.reader.read();
            if (ch < 0) return null;
            this.nextCharQueue.add((char)ch);
        }
        return this.nextCharQueue.element();
    }
    private Character peekNext(int pos) throws IOException {
        if (pos == 0) {
            return this.peekNext();
        }
        while (pos > 0) {
            if (this.nextCharQueue.size() <= pos) {
                this.nextCharQueue.get(pos);
            }
        }
        return null;
    }

    private TokenValue parseNextToken() throws IOException {
        var ch = this.peekNext();
        if (ch == null) {
            return new TokenValue(Token.EOF);
        }
        switch (ch) {
            case '+','-','*','/','m' -> {
                return this.readArithmeticOperator(ch);
            }
            case '<','>','=','\\','i' -> {
                return this.readComparisonOperator(ch);
            }
            case '.' -> {
                this.readNext();
                return new TokenValue(Token.POINT, ".");
            }
            case '[' -> {
                this.readNext();
                return new TokenValue(Token.LBRACKET, "[");
            }
            case ']' -> {
                this.readNext();
                return new TokenValue(Token.RBRACKET, "]");
            }
            case '(' -> {
                this.readNext();
                return new TokenValue(Token.LPARENT, "(");
            }
            case ')' -> {
                this.readNext();
                return new TokenValue(Token.RPARENT, ")");
            }
            case '%' -> {
                var comment = this.readComment(ch);
                return new TokenValue(Token.COMMENT, comment);
            }
            case ':' -> {
                this.readNext();
                ch = peekNext();
                if (ch == '-') {
                    this.readNext();
                    return new TokenValue(Token.UNIFY, ":-");
                } else {
                    throw new IOException(": must be followed by - for UNIFY but received '"+ch+"'");
                }
            }
            case '?' -> {
                this.readNext();
                ch = peekNext();
                if (ch == '-') {
                    this.readNext();
                    return new TokenValue(Token.QUERY, "?-");
                } else if (ch == '=') {
                    this.readNext();
                    return new TokenValue(Token.UNIFY_WITH_OCCUR_CHECK, "?=");
                } else {
                    throw new IOException("? must be followed by - for QUERY or = for UNIFY_WITH_OCCUR_CHECK but received '"+ch+"'");
                }
            }
            case ',' -> {
                this.readNext();
                return new TokenValue(Token.COMMA, ",");
            }
            case '\'' -> {
                this.readNext();
                var str = this.readQuotedAtom(ch);
                return new TokenValue(Token.QUOTED_ATOM, str);
            }
            case '"' -> {
                this.readNext();
                var str = this.readQuotedAtom(ch);

                return new TokenArrayValue(Token.ARRAY_OF_CHARACTERS, str);
            }
            case '0', '1', '2', '3', '4', '5', '6', '7', '8', '9' -> {
                var number = this.parseNumber(ch);
                return new TokenValue(Token.NUMBER, number);
            }
            case ' ', '\n', '\r', '\t' -> {
                this.readWhitespace(ch);
                return this.parseNextToken();
            }
            default -> {
                if (Character.isLetter(ch)) {
                    var str = this.readAtom(ch);
                    return new TokenValue(Token.ATOM, str);
                }
                throw new IOException("invalid character parsing '" + ch + "'");
            }
        }
    }

    // parse
    // <
    // = =< =\=
    // > >=
    // \=
    private TokenValue readComparisonOperator(Character ch) throws IOException {
        switch (ch) {
            case '<' -> {
                readNext();
                return new TokenValue(Token.COMPARISON_OPERATOR, "<");
            }
            case '>' -> {
                readNext();
                ch = this.peekNext();
                if (ch == '=') {
                    readNext();
                    return new TokenValue(Token.COMPARISON_OPERATOR, ">=");
                } else {
                    return new TokenValue(Token.COMPARISON_OPERATOR, ">");
                }
            }
            case '=' -> {
                readNext();
                ch = this.peekNext();
                if (ch == '\\') {
                    readNext();
                    ch = this.peekNext();
                    if (ch == '=') {
                        readNext();
                        return new TokenValue(Token.ARITHMETIC_INEQUALITY_OPERATOR, "=\\=");
                    } else {
                        throw new IOException("=\\ must be followed by = for NOT EQUAL '"+ch+"'");
                    }
                } else if (ch == '<') {
                    return new TokenValue(Token.COMPARISON_OPERATOR, "=<");
                } else {
                    return new TokenValue(Token.COMPARISON_OPERATOR, "=");
                }
            }
            case '\\' -> {
                readNext();
                ch = this.peekNext();
                if (ch == '=') {
                    readNext();
                    return new TokenValue(Token.COMPARISON_OPERATOR, "\\=");
                } else {
                    throw new IOException("\\ must be followed by = for NOT EQUAL '"+ch+"'");
                }
            }
            case 'i' -> {
                var atom = this.readAtom(ch);
                if (atom.equals("is")) {
                    return new TokenValue(Token.COMPARISON_OPERATOR, "is");
                } else {
                    return new TokenValue(Token.ATOM, atom);
                }
            }
            default -> throw new IOException("Illegal arithmetic operation '"+ch+"'");
        }
    }

    // parse + - * * / // mod
    private TokenValue readArithmeticOperator(Character ch) throws IOException {
        switch (ch) {
            case '+' -> {
                readNext();
                return new TokenValue(Token.ARITHMETIC_OPERATOR, "+");
            }
            case '-' -> {
                readNext();
                return new TokenValue(Token.ARITHMETIC_OPERATOR, "-");
            }
            case '/' -> {
                readNext();
                ch = this.peekNext();
                if (ch == '/') {
                    readNext();
                    return new TokenValue(Token.ARITHMETIC_OPERATOR, "//");
                } else {
                    return new TokenValue(Token.ARITHMETIC_OPERATOR, "/");
                }
            }
            case '*' -> {
                readNext();
                ch = this.peekNext();
                if (ch == '*') {
                    readNext();
                    return new TokenValue(Token.ARITHMETIC_OPERATOR, "**");
                } else {
                    return new TokenValue(Token.ARITHMETIC_OPERATOR, "*");
                }
            }
            case 'm' -> {
                var atom = this.readAtom(ch);
                if (atom.equals("mod")) {
                    return new TokenValue(Token.ARITHMETIC_OPERATOR, "*");
                } else {
                    return new TokenValue(Token.ATOM, atom);
                }
            }
            default -> throw new IOException("Illegal arithmetic operation '"+ch+"'");
        }
    }

    private String readComment(Character ch) throws IOException {
        var str = new StringBuilder();
        while (ch != null && ch != '\r' && ch != '\n') {
            str.append(ch);
            readNext();
            ch = this.peekNext();
        }
        if (ch != null) {
            var lastCr = ch;
            readNext();
            ch = this.peekNext();
            if (lastCr == '\r' && ch == '\n') {
                readNext();
            }
        }
        return str.toString();
    }

    private String readAtom(Character ch) throws IOException {
        var str = new StringBuilder();
        while (ch != null && Character.isLetterOrDigit(ch)) {
            str.append(ch);
            readNext();
            ch = this.peekNext();
        }
        return str.toString();
    }

    private boolean isControlCharacter(Character ch){
        return ch != null && "\"\\/bfnrt".contains(ch.toString());
    }

    private void parseConstant(Character ch, String constant) throws IOException {
        String foundConstant = this.readAnyChars(ch, constant);
        if (!constant.equals(foundConstant)) {
            throw new IOException("found invalid token '"+foundConstant+"' instead of '"+constant+"'");
        }
    }

    private Number parseNumber(Character ch) throws IOException {
        String nextNumberString = this.readAnyChars(ch, "0123456789.+-eE");
        if (nextNumberString.endsWith(".")) {
            this.nextCharQueue.add(0,'.');
            nextNumberString = nextNumberString.substring(0, nextNumberString.length()-1);
        }
        try {
            Matcher matcher = validNumberPattern.matcher(nextNumberString);
            if (matcher.matches()) {
                var number = NumberFormat.getInstance().parse(nextNumberString);
                if (number.getClass().equals(Double.class) && Double.isInfinite(number.doubleValue())) {
                    return new BigDecimal(nextNumberString);
                }
                return number;
            } else {
                throw new IOException("invalid number format '"+nextNumberString+"'");
            }
        } catch (ParseException e) {
            throw new IOException(e.getMessage());
        }
    }


    private String readQuotedAtom(Character quotes) throws IOException {
        var str = new StringBuilder();
        // dont add quotes
        var ch = this.peekNext();
        while (ch != null && !ch.equals(quotes)) {
            if (ch == '\\') { // is escaped
                str.append(ch);
                readNext();
                ch = peekNext();
                if (this.isControlCharacter(ch)) {
                    str.append(ch);
                    readNext();
                    ch = this.peekNext();
                }
            } else {
                str.append(ch);
                readNext();
                ch = this.peekNext();
            }
        }
        if (ch == null) {
            throw new IOException("Illegal string parsing - so far='"+str+"'");
        }
        // dont add quotes
        readNext();
        return str.toString();
    }

    private String readWhitespace(Character whitespace) throws IOException {
        return this.readAnyChars(whitespace, " \n\r\t");
    }

    private String readAnyChars(Character any, String nextChars) throws IOException {
        var str = new StringBuilder();
        str.append(any);
        var ch = this.peekNext();
        while (ch != null && nextChars.contains(ch.toString())) {
            str.append(ch);
            readNext();
            ch = this.peekNext();
        }
        if (ch == null) {
            return str.toString();
        }
        return str.toString();
    }

}