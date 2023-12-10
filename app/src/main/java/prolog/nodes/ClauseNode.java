package prolog.nodes;

import prolog.TokenValue;
import prolog.interpreter.Constr;
import prolog.interpreter.FreeVars;
import prolog.interpreter.PrologRuntime;
import prolog.interpreter.Term;

import java.io.IOException;
import java.util.*;

public class ClauseNode extends AbstractNode {

    public final FactNode fact;
    public final RuleNode rule;

    public final ExpressionNode query;

    public ClauseNode(FactNode fact) {
        this.fact = fact;
        this.rule = null;
        this.query = null;
    }
    public ClauseNode(RuleNode rule) {
        this.rule = rule;
        this.fact = null;
        this.query = null;
    }

    public ClauseNode(ExpressionNode query) {
        this.rule = null;
        this.fact = null;
        this.query = query;
    }

    @Override
    public void execute(PrologRuntime runtime) throws IOException {
        if (this.fact != null) {
            this.fact.execute(runtime);
        }
        if (this.rule != null) {
            this.rule.execute(runtime);
        }
        if (this.query != null) {
            this.query.execute(runtime);
        }
    }

    @Override
    public StringBuilder append(StringBuilder builder) {
        return this.fact != null ? this.fact.append(builder) : this.rule != null ? this.rule.append(builder) : this.query.append(builder);
    }

    private TermStatus termStatus() {
        if (this.fact != null) return this.fact;
        if (this.rule != null) return this.rule;
        return this.query;
    }

    @Override
    public boolean isGround() {
        if (this.query == null) return this.termStatus().isGround();
        return false;
    }

    @Override
    public boolean isPartiallyInstantiated() {
        return this.termStatus().isPartiallyInstantiated();
    }

    @Override
    public boolean isInstantiated() {
        return this.termStatus().isInstantiated();
    }

    @Override
    public boolean isUnInstantiated() {
        return this.termStatus().isUnInstantiated();
    }


    @Override
    public FreeVars freevars() {
        return this.fact != null ? this.fact.freevars() : this.rule != null ? this.rule.freevars() : this.query.freevars();
    }

    @Override
    public Optional<Constr> asConstr() {
        return this.fact != null ? this.fact.asConstr() : this.rule != null ? this.rule.asConstr() : this.query.asConstr();
    }

    public Term lhs() {
        return this.fact != null ? this.fact.predicate : this.rule != null ? this.rule.lhs() : this.query;
    }
    public List<Term> rhs() {
        return this.fact != null ? Term.EMPTY_LIST : this.rule != null ? this.rule.rhs() : Term.EMPTY_LIST;
    }


}
