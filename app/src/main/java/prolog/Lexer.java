package prolog;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Lexer {

    private static final String validNumberRegexp = "^-?(?:0\\d*|[1-9]\\d*)(?:\\.\\d+)?(?:[eE][+-]?\\d+)?$";

    private static Map<String, Character> ESCAPED0;

    static {
        // escaped: \' --> '     '' --> ''   \\ --> \   \" --> "   \% ---> %   \\n --> \n  \\t -> \t   \\r ---> \r

        ESCAPED0 = new HashMap<>();
        ESCAPED0.put("\\'", '\'');
        ESCAPED0.put("\\.", '.');
        ESCAPED0.put("''", '\'');
        ESCAPED0.put("\\\\", '\\');
        ESCAPED0.put("\\\"", '\"');
        ESCAPED0.put("\\%", '%');
        //ESCAPED0.put("\\n", '\n');
        //ESCAPED0.put("\\r", '\r');
        //ESCAPED0.put("\\t", '\t');
    }


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
        var token = this.parseNextToken();
        while (token != null && token.token != Token.EOF) {
            tokens.add(token);
            token = this.parseNextToken();
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

    // if lowercase --> atom
    // if numberlowercase -> atom
    // if Uppercasenumber -> variable
    // if lowercasenumber -> atom
    private TokenValue makeAtomOrVariable(String str, Token atomTokenType) throws IOException {
        if (Character.isUpperCase(str.charAt(0)) || str.charAt(0) == '_') {
            return new TokenValue(Token.VARIABLE, str);
        }
        if (Character.isLowerCase(str.charAt(0))) {
            return new TokenValue(atomTokenType, str);
        }
        if (Character.isDigit(str.charAt(0))) {
            return new TokenValue(atomTokenType, str);
        }
        return new TokenValue(atomTokenType, str);
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
            case '!' -> {
                this.readNext();
                return new TokenValue(Token.CUT, "!");
            }
            case '[' -> {
                this.readNext();
                ch = peekNext();
                if (ch == ']') {
                    this.readNext();
                    return TokenValue.NIL;
                } else {
                    return new TokenValue(Token.OPEN_LIST, "[");
                }
            }
            case ']' -> {
                this.readNext();
                return new TokenValue(Token.CLOSE_LIST, "]");
            }
            case '(' -> {
                this.readNext();
                return new TokenValue(Token.OPEN_PARENTHESIS, "(");
            }
            case ')' -> {
                this.readNext();
                return new TokenValue(Token.CLOSE_PARENTHESIS, ")");
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
            case '|' -> {
                this.readNext();
                return new TokenValue(Token.DECOMPOSITION_OPERATOR, "|");
            }
            case '_' -> {
                var atom = this.readAtom(ch);
                if (atom.equals("_")) {
                    return new TokenValue(Token.ANONYMOUS_VARIABLE, atom);
                } else {
                    return new TokenValue(Token.VARIABLE, atom);
                }
            }
            case ';' -> {
                this.readNext();
                return new TokenValue(Token.SEMICOLON, ";");
            }
            case '\'' -> {
                this.readNext();
                return makeAtomOrVariable(this.readQuotedAtom(ch), Token.QUOTED_ATOM);
            }
            case '"' -> {
                this.readNext();
                var str = this.readQuotedAtom(ch);

                return new TokenArrayValue(Token.ARRAY_OF_CHARACTERS, str);
            }
            case '0', '1', '2', '3', '4', '5', '6', '7', '8', '9' -> {
                // 0' character codes. use next char as atom
                if (ch == '0') {
                    var firstChar = ch;
                    this.readNext();
                    ch = peekNext();
                    if (ch == '\'') {
                        this.readNext();
                        ch = peekNext();
                        return readUnicodeEncodedChar(ch);
                    } else {
                        var number = this.parseNumber(firstChar, ch);
                        return new TokenValue(Token.NUMBER, number);
                    }
                } else {
                    var number = this.parseNumber(null, ch);
                    return new TokenValue(Token.NUMBER, number);
                }
            }
            case ' ', '\n', '\r', '\t' -> {
                this.readWhitespace(ch);
                return this.parseNextToken();
            }
            default -> {
                if (Character.isLetter(ch)) {
                    return makeAtomOrVariable(this.readAtom(ch), Token.ATOM);
                }
                throw new IOException("invalid character parsing '" + ch + "'");
            }
        }
    }
    private int convertUnicodeEscape(String str) throws IOException {
        if (str.startsWith("\\u")) {
            return Integer.parseInt(str.substring(2), 16);
        } else {
            return Integer.parseInt(str, 16);
        }
    }


    // parse escaped chars
    // prefix 0x: next char is return as number
    // escaped: \' --> '     '' --> ''   \\ --> \   \" --> "   \% ---> %   \\n --> \n  \\t -> \t   \\r ---> \r
    // unicode \u0000 --> int using convertUnicodeEscape
    private TokenValue readUnicodeEncodedChar(Character ch) throws IOException {
        if (ch == '\\') {
            this.readNext();
            ch = this.peekNext();
            if (ch == 'u') {
                this.readNext();
                ch = this.peekNext();
                var codeString = this.readAnyChars(ch, "0123456789");
                var code = this.convertUnicodeEscape("\\u" + codeString);
                return new TokenValue(Token.NUMBER, code);
            } else {
                var lookup = "\\" + String.valueOf(ch);
                var escapeChar = ESCAPED0.get(lookup);
                if (escapeChar != null) {
                    this.readNext();
                    return new TokenValue(Token.NUMBER, (int) escapeChar);
                } else {
                    throw new IOException("invalid escaped code 0' using >\\" + ch + "<");
                }
            }
        } else if (ch == '\'') {
            this.readNext();
            ch = this.peekNext();
            var lookup = "\'" + ch;
            var escapeChar = ESCAPED0.get(lookup);
            if (escapeChar != null) {
                this.readNext();
                return new TokenValue(Token.NUMBER, (int) escapeChar);
            } else {
                throw new IOException("invalid ' escaped code 0''_ using >" + ch + "<");
            }
        } else {
            // use ch as single escaped atom "0' " --> " " (space)
            this.readNext();
            return new TokenValue(Token.NUMBER, (int)ch);
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
                return new TokenValue(Token.BINARY_COMPARISON_OPERATOR, "<");
            }
            case '>' -> {
                readNext();
                ch = this.peekNext();
                if (ch == '=') {
                    readNext();
                    return new TokenValue(Token.BINARY_COMPARISON_OPERATOR, ">=");
                } else {
                    return new TokenValue(Token.BINARY_COMPARISON_OPERATOR, ">");
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
                        return new TokenValue(Token.ARITHMETIC_INEQUALITY_BINARY_OPERATOR, "=\\=");
                    } else {
                        throw new IOException("=\\ must be followed by = for NOT EQUAL '" + ch + "'");
                    }
                } else if (ch == ':') {
                        readNext();
                        ch = this.peekNext();
                        if (ch == '=') {
                            readNext();
                            return new TokenValue(Token.ARITHMETIC_EQUALITY_BINARY_OPERATOR, "=:=");
                        } else {
                            throw new IOException("=: must be followed by = for EQUAL '"+ch+"'");
                        }
                } else if (ch == '<') {
                    return new TokenValue(Token.BINARY_COMPARISON_OPERATOR, "=<");
                } else {
                    return new TokenValue(Token.BINARY_COMPARISON_OPERATOR, "=");
                }
            }
            case '\\' -> {
                readNext();
                ch = this.peekNext();
                if (ch == '=') {
                    readNext();
                    return new TokenValue(Token.BINARY_COMPARISON_OPERATOR, "\\=");
                } else if (ch == '+') {
                        readNext();
                        return new TokenValue(Token.UNARY_NOT_OPERATOR, "\\+");
                } else {
                    throw new IOException("\\ must be followed by = for NOT EQUAL '"+ch+"'");
                }
            }
            case 'i' -> {
                var atom = this.readAtom(ch);
                if (atom.equals("is")) {
                    return new TokenValue(Token.ARITHMETIC_UNIFY_BINARY_OPERATOR, "is");
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
                } else if (ch == '*') {
                    readNext();
                    ch = this.peekNext();
                    var comment = this.readLongComment(ch);
                    return new TokenValue(Token.COMMENT, comment);
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

    private String readLongComment(Character ch) throws IOException {
        var str = new StringBuilder();
        str.append("/*");
        var endComment = false;
        do {
            while (ch != null && ch != '*') {
                str.append(ch);
                readNext();
                ch = this.peekNext();
            }
            if (ch != null) {
                var lastCr = ch;
                str.append(ch);
                readNext();
                ch = this.peekNext();
                str.append(ch);
                if (lastCr == '*' && ch == '/') {
                    readNext();
                    endComment = true;
                }
            }
        } while (!endComment);
        return str.toString();
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
        while (ch != null && (Character.isLetterOrDigit(ch) || ch == '_')) {
            str.append(ch);
            readNext();
            ch = this.peekNext();
        }
        return str.toString();
    }

    private boolean isControlCharacter(Character ch){
        return ch != null && "\"\\/bfnrt".contains(ch.toString());
    }

    private Number parseNumber(Character prefixChars, Character ch) throws IOException {
        String nextNumberString = this.readAnyChars(ch, "0123456789.+-eE_", validNumberPattern);
        if (prefixChars != null) {
            nextNumberString = prefixChars + nextNumberString;
        }
        if (nextNumberString.endsWith(".")) {
            this.nextCharQueue.add(0,'.');
            nextNumberString = nextNumberString.substring(0, nextNumberString.length()-1);
        }
        nextNumberString = nextNumberString.replace("_", "");
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

    private boolean isNotAllowedQuotedChars(Character quotes, Character ch) {
        if (quotes.equals('\'')) {
            return ch == '\t' || ch == '\r' || ch == '\n';
        } else {
            return false;
        }
    }

    private String readQuotedAtom(Character quotes) throws IOException {
        var str = new StringBuilder();
        // don't add quotes
        var ch = this.peekNext();
        var lastWasEscaped = false;
        while (ch != null && !ch.equals(quotes)) {
            if (ch == '\\') { // is escaped
                lastWasEscaped = true;
                str.append(ch);
                readNext();
                ch = peekNext();
                if (this.isControlCharacter(ch)) {
                    lastWasEscaped = false;
                    str.append(ch);
                    readNext();
                    ch = this.peekNext();
                }
            } else if (lastWasEscaped && this.isNotAllowedQuotedChars(quotes, ch)) {
                throw new IOException("Illegal special character ='"+ch+"'");
            } else {
                lastWasEscaped = false;
                str.append(ch);
                readNext();
                ch = this.peekNext();
            }
        }
        if (ch == null) {
            throw new IOException("Illegal string parsing - so far='"+str+"'");
        }
        // don't add quotes
        readNext();
        return str.toString();
    }

    private String readWhitespace(Character whitespace) throws IOException {
        return this.readAnyChars(whitespace, " \n\r\t");
    }

    private String readAnyChars(Character any, String nextChars) throws IOException {
        return this.readAnyChars(any, nextChars, null);
    }

    private String readAnyChars(Character ch, String nextChars, Pattern pattern) throws IOException {
        var str = new StringBuilder();
        while (ch != null && nextChars.contains(ch.toString())) {
            var tmp = str.toString()+ch;
            if (pattern != null && !pattern.matcher(tmp).matches()) {
                break;
            }
            str.append(ch);
            readNext();
            ch = this.peekNext();
        }
        return str.toString();
    }

}
