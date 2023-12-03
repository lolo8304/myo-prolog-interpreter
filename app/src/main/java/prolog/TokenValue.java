package prolog;

public class TokenValue {

    public static TokenValue NIL = new TokenValue(Token.nil, "[]");

    public final Token token;
    public final Object value;
    public final String string;

    public TokenValue(Token token) {
        this(token, null);
    }
    public TokenValue(Token token, Object value) {
        this.token = token;
        this.value = value;
        this.string = value != null ? value.toString() : "";
    }

    @Override
    public String toString() {
        return "["+this.token+"="+this.toValueString()+"]";
    }
    public String toValueString() {
        return this.string;
    }

    public boolean isNotAnyOf(Token... tokens) {
        return !this.is(tokens);
    }

    public boolean is(Token... tokens) {
        if (tokens == null || tokens.length == 0) return false;
        for (int i = 0; i < tokens.length; i++) {
            if (this.token == tokens[i]) {
                return true;
            }
        }
        return false;
    }

    public boolean isComparisonOperator() {
        return this.is(Token.BINARY_COMPARISON_OPERATOR, Token.ARITHMETIC_EQUALITY_BINARY_OPERATOR, Token.ARITHMETIC_INEQUALITY_BINARY_OPERATOR);
    }
    public boolean isLogicalAndArithmeticOperator() {
        return this.is(Token.COMMA, Token.SEMICOLON, Token.ARITHMETIC_UNIFY_BINARY_OPERATOR, Token.ARITHMETIC_OPERATOR);
    }
}
