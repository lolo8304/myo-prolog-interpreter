package prolog.interpreter;

import prolog.Token;
import prolog.TokenValue;

import java.util.*;
import java.util.stream.Stream;

public class Var implements Term {


    public final TokenValue atom;
    private final FreeVars freevars;

    public Var(TokenValue atom) {
        this.atom = atom;
        this.freevars = FreeVars.of(atom);
    }

    public String name() {
        return this.atom.toValueString();
    }

    private boolean isAnonymous() {
        return this.atom.is(Token.ANONYMOUS_VARIABLE);
    }

    @Override
    public FreeVars freevars() {
        return this.freevars;
    }

    @Override
    public Term map(Subst s) {
        if (this.isAnonymous()) {
            return this;
        }
        return s.lookup(this.name()).map(value -> value.map(s)).orElse(this);
    }

    @Override
    public Optional<Subst> pmatch(Term term, Subst s) {
        if (this.isAnonymous()) {
            return Optional.of(s);
        }
        var term1 = s.lookup(this.name());
        if (term1.isPresent()) {
            return term1.get().pmatch(term,s);
        } else {
            return Optional.of(new Subst(new Binding(this.name(), this), s));
        }
    }

    @Override
    public Optional<Subst> unify(Term y, Subst s) {
        if (this.isAnonymous()) {
            return Optional.of(s);
        }
        var termX1 = s.lookup(this.name());
        if (termX1.isPresent()) {
            return termX1.get().unify(y, s);
        }

        var yAsVar = y.asVar();
        if (yAsVar.isPresent()) {
            var yVar = yAsVar.get();
            if (yVar.isAnonymous() || this.name().equals(yVar.name())) {
                return Optional.of(s);
            }

            var termY1 = s.lookup(yVar.name());
            if (termY1.isPresent()) {
                return this.unify(termY1.get(), s);
            }

            return Optional.of(new Subst(new Binding(this.name(), yVar), s));
        } else {
            // this = var, x = not
            if (y.map(s).freevars().contains(this.atom)) {
                return Optional.empty();
            } else {
                return Optional.of(new Subst(new Binding(this.name(),y), s));
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
    public Terms concat(Term term) {
        return new TermsList(this.asTerm(), term);
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
