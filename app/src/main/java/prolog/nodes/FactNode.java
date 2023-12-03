package prolog.nodes;

import prolog.Memory;
import prolog.Prolog;
import prolog.PrologRuntime;

import java.io.IOException;
import java.util.Objects;

public class FactNode extends AbstractNode {
    public final PredicateNode predicate;

    public FactNode(PredicateNode predicate) {
        this.predicate = predicate;
    }
    @Override
    public void execute(PrologRuntime runtime) throws IOException {
        if (this.isGoal()) {
            runtime.memory.addFact(this);
            System.out.println("true");
        } else {
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

    @Override
    public int hashCode() {
        return Objects.hash(this.key());
    }

    public String key() {
        return this.predicate.key();
    }

    public boolean isGoal() {
        return this.predicate.isGoal();
    }
}
