package prolog.interpreter;

import java.util.ArrayList;
import java.util.Collection;
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
        var builder = new StringBuilder();
        var second = false;
        for (var binding: this) {
            if (second) builder.append("\n; ");
            second = true;
            builder.append(binding.name).append("=").append(binding.term);
        }
        return builder.toString();
    }
}
