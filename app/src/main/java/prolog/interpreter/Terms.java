package prolog.interpreter;

import prolog.nodes.AbstractNode;
import prolog.nodes.Node;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public class Terms extends ArrayList<Term> {
    public final static Terms EMPTY_TERMS = new Terms();

    public Terms(int initialCapacity) {
        super(initialCapacity);
    }

    public Terms() {
    }

    public Terms(Collection<? extends Term> c) {
        super(c);
    }

    public Terms(List<AbstractNode> nodes) {
        super(nodes.stream().map(AbstractNode::asTerm).toList());
    }

    @Override
    public String toString() {
        var builder = new StringBuilder();
        var second = false;
        for (var term: this) {
            if (second) builder.append(", ");
            second = true;
            builder = term.append(builder);
        }
        return builder.toString();
    }
}
