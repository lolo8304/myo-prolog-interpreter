package prolog.interpreter;

import prolog.TokenValue;
import prolog.nodes.AbstractNode;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public class TermsVar extends TermsList {

    private TokenValue variable;

    public TermsVar(TokenValue variable) {
        super();
        this.variable = variable;
        this.freevars().add(this.variable);
    }
    public TermsVar(Var variable) {
        super();
        this.variable = variable.atom;
        this.freevars().add(this.variable);
    }

    @Override
    public Term lhs() {
        throw new RuntimeException("Var as list has no lhs");
    }

    @Override
    public TermsList rhs() {
        throw new RuntimeException("Var as list has no lhs");
    }

    @Override
    public Optional<Var> asVar() {
        return this.variable.asVar();
    }

    @Override
    public Terms map(Subst s) {
        var mappedTerm = this.variable.map(s);
        if (mappedTerm instanceof Terms termAsTerms) return termAsTerms;
        if (mappedTerm instanceof Var termAsVar) return new TermsVar(termAsVar);
        throw new RuntimeException("Mapped term is NOT a list of terms or var");
    }

    @Override
    public Optional<Subst> unify(Term y, Subst s) {
        return this.variable.unify(y, s);
    }

    @Override
    public Optional<Constr> asConstr() {
        return Optional.empty();
    }

    @Override
    public Term asTerm() {
        return this.asVar().orElseThrow();
    }

    @Override
    public TermsList concat(Terms terms) {
        throw new RuntimeException("concat not allowed for var as list");
    }

    @Override
    public Optional<Subst> unify(Terms ys, Subst s) {
        return this.variable.unify(ys, s);
    }

    @Override
    public Optional<Subst> pmatch(Terms ys, Subst s) {
        return this.variable.pmatch(ys, s);
    }

    @Override
    public Optional<Subst> pmatch(Term term, Subst s) {
        return this.variable.pmatch(term, s);
    }
}
