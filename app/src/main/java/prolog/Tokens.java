package prolog;

public class Tokens {
    private final TokenValue[] tokens;

    public Tokens(TokenValue... tokens) {
        this.tokens = tokens;
    }

    @Override
    public String toString() {
        var builder = new StringBuilder();
        builder.append("[");
        if (this.tokens.length > 0) {
            builder.append(" ");
        }
        for (var token : this.tokens) {
            builder.append(token.toValueString());
            builder.append(" ");
        }
        builder.append("]");
        return builder.toString();
    }

    public int size() {
        return this.tokens.length;
    }
}
