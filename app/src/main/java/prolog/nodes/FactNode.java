package prolog.nodes;

import prolog.TokenValue;
import prolog.interpreter.*;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class FactNode extends AbstractNode implements Term {
    public final PredicateNode predicate;

    public FactNode(PredicateNode predicate) {
        this.predicate = predicate;
    }
    @Override
    public void execute(PrologRuntime runtime) throws IOException {
        if (this.isGround()) {
            runtime.top().memory.addFact(this);
        }
        if (runtime.inQueryMode()) {
            runtime.findSolution(this);
        }
    }

    @Override
    public StringBuilder append(StringBuilder builder) {
        return this.predicate.append(builder);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if ((o instanceof String str)) return Objects.equals(this.key(), str);
        if (!(o instanceof FactNode factNode)) return false;
        return Objects.equals(this.key(), factNode.key());
    }

    public String predicateIndicator() {
        return this.predicate.predicateIndicator();
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.key());
    }

    public String key() {
        return this.predicate.key();
    }

    @Override
    public boolean isGround() {
        return this.predicate.isGround();
    }

    @Override
    public boolean isPartiallyInstantiated() {
        return this.predicate.isPartiallyInstantiated();
    }

    @Override
    public boolean isInstantiated() {
        return this.predicate.isInstantiated();
    }

    @Override
    public boolean isUnInstantiated() {
        return this.predicate.isUnInstantiated();
    }

    @Override
    public FreeVars freevars() {
        return this.predicate.freevars();
    }

    @Override
    public Term map(Subst s) {
        return this.predicate.map(s);
    }

    @Override
    public Optional<Subst> pmatch(Term term, Subst s) {
        return this.predicate.pmatch(term, s);
    }

    @Override
    public Optional<Subst> unify(Term y, Subst s) {
        return this.predicate.unify(y,s);
    }

    @Override
    public Optional<Constr> asConstr() {
        return this.predicate.asConstr();
    }

    @Override
    public Optional<Var> asVar() {
        return this.predicate.asVar();
    }
}
