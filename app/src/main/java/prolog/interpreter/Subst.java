package prolog.interpreter;

import prolog.Token;
import prolog.TokenValue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public class Subst extends ArrayList<Binding> {

    public Subst(int initialCapacity) {
        super(initialCapacity);
    }

    public Subst() {
    }

    public Subst(Collection<? extends Binding> c) {
        super(c);
    }

    public Subst(Binding binding, Subst s) {
        this(s);
        this.add(0, binding);
    }
    public Subst(Binding binding) {
        this(new Subst());
        this.add(0, binding);
    }

    public Optional<Term> lookup(String name) {
        for (var binding : this) {
            if (binding.name.equals(name)) {
                return Optional.of(binding.term);
            }
        }
        return Optional.empty();
    }

    @Override
    public String toString() {
        return this.toString(this.stream().map(binding -> binding.name).toList());
    }

    public String toString(FreeVars variables) {
        return this.toString(variables.stream()
                .filter(token -> !token.is(Token.ANONYMOUS_VARIABLE))
                .map(TokenValue::toValueString)
                .toList());
    }

    private String toString(List<String> variableNames) {
        var builder = new StringBuilder();
        var second = false;
        for (var variableName: variableNames) {
            var term = this.lookup(variableName);
            if (term.isEmpty()) {
                continue;
            }
            if (second) builder.append("\n; ");
            second = true;
            builder.append(variableName).append("=").append(term.get().map(this));
        }
        return builder.toString();
    }
}
