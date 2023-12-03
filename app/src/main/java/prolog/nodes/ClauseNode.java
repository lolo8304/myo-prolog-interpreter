package prolog.nodes;

import prolog.Memory;
import prolog.PrologRuntime;
import prolog.nodes.Node;

import java.io.IOException;
import java.util.Optional;

public class ClauseNode extends AbstractNode {

    public final FactNode fact;
    public final RuleNode rule;

    public ClauseNode(FactNode fact) {
        this.fact = fact;
        this.rule = null;
    }
    public ClauseNode(RuleNode rule) {
        this.rule = rule;
        this.fact = null;
    }
    @Override
    public void execute(PrologRuntime runtime) throws IOException {
        if (this.fact != null) {
            this.fact.execute(runtime);
        }
        if (this.rule != null) {
            this.rule.execute(runtime);
        }
    }

    @Override
    public StringBuilder append(StringBuilder builder) {
        return this.fact != null ? this.fact.append(builder) : this.rule.append(builder);
    }

    public boolean isGoal() {
        return this.fact != null && this.fact.isGoal() ||
                this.rule != null && this.rule.isGoal();
    }

}
