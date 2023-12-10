package prolog.nodes;

import prolog.Prolog;
import prolog.Token;
import prolog.TokenValue;
import prolog.interpreter.*;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class RuleNode extends AbstractNode implements Term {
    public final PredicateNode head;
    public final ExpressionNode body;

    public RuleNode(PredicateNode head, ExpressionNode body) {
        this.head = head;
        this.body = body;
    }
    @Override
    public void execute(PrologRuntime runtime) throws IOException {
        if (this.isGround()) {
            runtime.top().memory.addRule(this);
        }
        if (runtime.inQueryMode()) {
            runtime.findSolution(this);
        }


        runtime.memory().addRule(this);
        if (this.isGround()) {
            if (Prolog.verbose()) {
                System.out.println("true");
            }
        }

    }

    @Override
    public StringBuilder append(StringBuilder builder) {
        return this.body.append(this.head.append(builder).append(":-"));
    }

    public String key() {
        return this.head.key();
    }
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if ((o instanceof String str)) return Objects.equals(this.key(), str);
        if (!(o instanceof RuleNode ruleNode)) return false;
        return Objects.equals(this.key(), ruleNode.key());
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.key());
    }

    @Override
    public boolean isGround() {
        return this.head.isGround() && this.body.isGround();
    }

    // at least 1 must be Uninstantiated
    @Override
    public boolean isPartiallyInstantiated() {
        return this.head.isUnInstantiated() || this.head.isPartiallyInstantiated()
                || this.body.isUnInstantiated() || this.body.isPartiallyInstantiated();
    }

    @Override
    public boolean isInstantiated() {
        return this.head.isInstantiated() && this.body.isInstantiated();
    }

    @Override
    public boolean isUnInstantiated() {
        return this.head.isUnInstantiated() && this.body.isUnInstantiated();
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
        return this.asConstr().get().unify(y,s);
    }

    @Override
    public Optional<Constr> asConstr() {
        return Optional.of(new Constr(new TokenValue(Token.UNIFY, ":-"), Arrays.asList(this.head, this.body)));
    }

    @Override
    public Optional<Var> asVar() {
        return Optional.empty();
    }

    public PredicateNode lhs() {
        return this.head;
    }

    public List<Term> rhs() {
        return this.body.rhs();
    }
}
