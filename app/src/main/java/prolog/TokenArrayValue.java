package prolog;

import java.util.Arrays;

public class TokenArrayValue extends  TokenValue{
    private final int[] array;

    public TokenArrayValue(Token token) {
        super(token);
        this.array = new int[0];
    }

    public TokenArrayValue(Token token, String string) {
        super(token);
        this.array = string.chars().toArray();
    }
    public TokenArrayValue(Token token, int[] array) {
        super(token);
        this.array = array;
    }

    @Override
    public String toString() {
        var builder = new StringBuilder();
        builder.append("[");
        for (int i = 0; i < this.array.length; i++) {
            if (i > 0) builder.append(", ");
            builder.append(this.array[i]);
        }
        builder.append("]");
        return builder.toString();
    }

    @Override
    public String toValueString() {
        return this.toString();
    }
}
