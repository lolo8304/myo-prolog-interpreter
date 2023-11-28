package prolog;

public class TokenValue {
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
}
