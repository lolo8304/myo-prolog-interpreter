package prolog.interpreter;

import prolog.TokenValue;

import java.util.*;
import java.util.stream.Stream;

public class Var implements Term {


    private final TokenValue atom;
    private final FreeVars freevars;

    public Var(TokenValue atom) {
        this.atom = atom;
        this.freevars = FreeVars.of(atom);
    }

    public String name() {
        return this.atom.toValueString();
    }

    @Override
    public FreeVars freevars() {
        return this.freevars;
    }

    @Override
    public Term map(Subst s) {
        return s.lookup(this.name()).map(value -> value.map(s)).orElse(this);
    }

    @Override
    public Optional<Subst> pmatch(Term term, Subst s) {
        var term1 = s.lookup(this.name());
        if (term1.isPresent()) {
            return term1.get().pmatch(term,s);
        } else {
            return Optional.of(new Subst(new Binding(this.name(), this), s));
        }
    }

    @Override
    public Optional<Subst> unify(Term y, Subst s) {
        var yAsVar = y.asVar();
        if (yAsVar.isPresent()) {
            if (this.name().equals(yAsVar.get().name())) {
                return Optional.of(s);
            } else {
                return Optional.empty();
            }
        } else {
            var termX1 = s.lookup(this.name());
            if (termX1.isPresent()) {
                return termX1.get().unify(y, s);
            } else {
                if (y.map(s).freevars().contains(this.atom)) {
                    return Optional.empty();
                } else {
                    return Optional.of(new Subst(new Binding(this.name(),y), s));
                }
            }
        }
    }

    @Override
    public Optional<Constr> asConstr() {
        return Optional.empty();
    }

    @Override
    public Optional<Var> asVar() {
        return Optional.of(this);
    }

    @Override
    public Term asTerm() {
        return this;
    }

    @Override
    public StringBuilder append(StringBuilder builder) {
        builder.append(this.atom);
        if (this.freevars.contains(this.atom)) {
            builder.append("*");
        }
        return builder;
    }

    @Override
    public String toString() {
        return this.append(new StringBuilder()).toString();
    }
}
