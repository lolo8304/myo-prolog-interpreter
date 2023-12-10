package prolog.interpreter;

import prolog.TokenValue;

import java.util.*;

public class FreeVars extends ArrayList<TokenValue> {


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
        return new Subst(this.stream().map(TokenValue::asBinding).toList());
    }
}
