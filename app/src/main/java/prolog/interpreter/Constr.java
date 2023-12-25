package prolog.interpreter;

import prolog.TokenValue;
import prolog.nodes.PredicateNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class Constr implements  Term {

    public final List<Term> EMPTY_TERMS = new ArrayList<>();

    public final TokenValue atom;
    private final FreeVars freevars;
    public final Terms terms;

    public Constr(TokenValue atom, List<Term> terms) {
        this(atom, new TermsList(terms));
    }

    public Constr(TokenValue atom, Terms terms) {
        this.atom = atom;
        this.terms = terms;
        this.freevars = new FreeVars(terms.stream().flatMap(x -> x.freevars().stream()).toList());
    }

    public Constr(TokenValue atom) {
        this(atom, Terms.EMPTY_TERMS);
    }


    public Constr(PredicateNode predicate) {
        this.atom = predicate.atom;
        this.terms = new TermsList(predicate.arguments);
        this.freevars = new FreeVars(predicate.arguments.stream().flatMap(x -> x.freevars().stream()).toList());
    }

    @Override
    public FreeVars freevars() {
        return this.freevars;
    }

    @Override
    public Term map(Subst s) {
        return new Constr(this.atom, this.terms.stream().map( x -> x.map(s)).toList());
    }

    @Override
    public Optional<Subst> pmatch(Term term, Subst s) {
        var termAsConstr = term.asConstr();
        if (termAsConstr.isPresent() && this.atom.equals(termAsConstr.get().atom) && this.terms.size() == termAsConstr.get().terms.size()) {
            return Constr.unify(this.terms, termAsConstr.get().terms, s);
        } else {
            return Optional.empty();
        }
    }

    @Override
    public Optional<Subst> unify(Term y, Subst s) {
        var yAsVar = y.asVar();
        if (yAsVar.isPresent()) {
            return yAsVar.get().unify(this, s);
        }
        var yAsConstr = y.asConstr();
        if (yAsConstr.isPresent()){
            if (this.atom.equals(yAsConstr.get().atom)) {
                return Constr.unify(this.terms, yAsConstr.get().terms, s);
            } else {
                return Optional.empty();
            }
        } else {
            return y.unify(this, s);
        }

    }

    @Override
    public Optional<Constr> asConstr() {
        return Optional.of(this);
    }

    @Override
    public Optional<Var> asVar() {
        return Optional.empty();
    }

    @Override
    public Term asTerm() {
        return this.asConstr().get();
    }


    public static Optional<Subst> pmatch(List<Term> patterns, List<Term> terms, Subst s) {
        if (patterns.size() != terms.size()) return Optional.empty();
        var newSubst = s;
        for (int i = 0; i < patterns.size(); i++) {
            var pattern = patterns.get(i);
            var term = terms.get(i);
            var optionalS = pattern.unify(term, s);
            if (optionalS.isEmpty()) return Optional.empty();
            newSubst = optionalS.get();
        }
        return Optional.of(newSubst);
    }

    public static Optional<Subst> unify(Terms xs, Terms ys, Subst s) {
        if (xs.size() != ys.size()) return Optional.empty();
        var newSubst = s;
        for (int i = 0; i < xs.size(); i++) {
            var x = xs.get(i);
            var y = ys.get(i);
            var optionalS = x.unify(y, newSubst);
            if (optionalS.isEmpty()) {
                optionalS = y.unify(x, newSubst);
            }
            if (optionalS.isPresent()) {
                newSubst = optionalS.get();
            }
        }
        return Optional.of(newSubst);
    }

    @Override
    public String toString() {
        return this.append(new StringBuilder()).toString();
    }

    @Override
    public StringBuilder append(StringBuilder builder) {
        builder.append(this.atom.toValueString());
        if (this.terms.size() == 0) return builder;

        builder.append("(");
        var second = false;
        for (var term: this.terms) {
            if (second) {
                builder.append(", ");
            }
            builder = term.append(builder);
            if (this.freevars.stream().anyMatch(x -> x.toValueString().equals(term.toString()))) {
                builder.append("*");
            }
            second=true;
        }
        builder.append(")");
        return builder;
    }

    public Term lhs() {
        return this.terms.lhs();
    }

    public Terms rhs() {
        return this.terms.rhs();
    }
}
