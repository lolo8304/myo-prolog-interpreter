package prolog.nodes;

import prolog.Token;
import prolog.interpreter.*;
import prolog.TokenValue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class ConditionNode extends AbstractNode implements Term {

    public final List<ArgumentNode> terms;
    public final List<TokenValue> conditions;
    private String _key;


    public ConditionNode(List<ArgumentNode> terms, List<TokenValue> conditions) {
        if (terms.size() != conditions.size()+1) {
            throw new AssertionError("Terms must be 1 more than conditions");
        }
        this.terms = terms;
        this.conditions = conditions;
    }
    @Override
    public void execute(PrologRuntime runtime) {

    }

    @Override
    public StringBuilder append(StringBuilder builder) {
        builder = this.terms.get(0).append(builder);
        for (int i = 0; i < this.conditions.size(); i++) {
            builder.append(this.conditions.get(i).toValueString());
            builder.append(this.terms.get(i+1));
        }
        return builder;
    }

    @Override
    public Term map(Subst s) {
        return this.asConstr().get().map(s);
    }

    @Override
    public Optional<Subst> pmatch(Term term, Subst s) {
        return this.asConstr().get().pmatch(term, s);
    }

    @Override
    public Optional<Subst> unify(Term y, Subst s) {
        return this.asConstr().get().unify(y, s);
    }

    @Override
    public Optional<Constr> asConstr() {
        return Optional.of(new Constr(this.conditions.get(0), new ArrayList<>(this.terms)));
    }

    @Override
    public Optional<Var> asVar() {
        return Optional.empty();
    }

    public List<Term> rhs() {
        return this.asConstr().get().terms;
    }

    @Override
    public boolean isGround() {
        return this.terms.stream().allMatch(x -> x.isInstantiated() || !this.isPartiallyInstantiated());
    }

    @Override
    public boolean isPartiallyInstantiated() {
        return this.terms.stream().anyMatch(ArgumentNode::isUnInstantiated);
    }

    @Override
    public boolean isInstantiated() {
        return this.terms.stream().allMatch(ArgumentNode::isInstantiated);
    }

    @Override
    public boolean isUnInstantiated() {
        return false;
    }

    public String resetKey() {
        this._key = null;
        return this.key();
    }
    public String key() {
        if (this._key != null) return this._key;
        this._key = this.append(new StringBuilder()).toString();
        return this._key;
    }
}
