package prolog.nodes;

import prolog.Prolog;
import prolog.PrologRuntime;

import java.io.IOException;
import java.util.Objects;

public class RuleNode extends AbstractNode {
    public final PredicateNode head;
    public final ExpressionNode body;

    public RuleNode(PredicateNode head, ExpressionNode body) {
        this.head = head;
        this.body = body;
    }
    @Override
    public void execute(PrologRuntime runtime) throws IOException {
        runtime.memory.addRule(this);
        if (this.isGoal()) {
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

    public boolean isGoal() {
        return this.head.isGoal() && this.body.isGoal();
    }
}
