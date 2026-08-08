package prolog.interpreter;

import prolog.Token;
import prolog.TokenValue;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

public class FreeVars extends ArrayList<TokenValue> {

    private static final AtomicLong NEXT_VAR_ID = new AtomicLong();

    public FreeVars(int initialCapacity) {
        super(initialCapacity);
    }

    public FreeVars() {
        super();
    }

    public FreeVars(Collection<? extends TokenValue> c) {
        this();
        this.addAll(c);
    }

    public static FreeVars of(TokenValue... tokenValues) {
        return new FreeVars(Arrays.stream(tokenValues).toList());
    }

    @Override
    public boolean add(TokenValue tokenValue) {
        if (tokenValue == null || this.contains(tokenValue)) return false;
        return super.add(tokenValue);
    }

    @Override
    public boolean addAll(Collection<? extends TokenValue> c) {
        var changed = false;
        for (var celem: c) {
            changed = this.add(celem) || changed;
        }
        return changed;
    }

    public Subst asSubs() {
        return new Subst(this.stream()
                .filter(token -> !token.is(Token.ANONYMOUS_VARIABLE))
                .map(token -> new Binding(
                        token,
                        new Var(new TokenValue(Token.VARIABLE, token.toValueString() + "_" + NEXT_VAR_ID.incrementAndGet()))))
                .toList());
    }

    public FreeVars concat(FreeVars freeVars) {
        var copy = new FreeVars(this);
        copy.addAll(freeVars);
        return copy;
    }

}
