package prolog.interpreter;

import prolog.nodes.AbstractNode;
import prolog.nodes.Node;

import java.util.*;

public class Terms extends ArrayList<Term> {
    public final static Terms EMPTY_TERMS = new Terms();

    public Terms(int initialCapacity) {
        super(initialCapacity);
    }

    public Terms() {
    }
    public Terms(Term... terms) {
        super();
        this.addAll(Arrays.stream(terms).toList());
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

    public Term lhs() {
        return this.get(0);
    }
    public Terms rhs() {
        return new Terms(this.subList(1,this.size()));
    }
}
