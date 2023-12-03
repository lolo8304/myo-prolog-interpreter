package prolog.nodes;

import prolog.PrologRuntime;
import prolog.TokenValue;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class PredicateNode extends AbstractNode {
    public final TokenValue atom;
    public final List<ArgumentNode> arguments;

    private String _key;

    public PredicateNode(TokenValue atom) {
        this.atom = atom;
        this.arguments = new ArrayList<>();
        this._key = null;
    }

    public PredicateNode addArgument(ArgumentNode argument) {
        this.arguments.add(argument);
        this._key = null;
        return this;
    }
    @Override
    public void execute(PrologRuntime runtime) {

    }

    @Override
    public StringBuilder append(StringBuilder builder) {
        builder.append(this.atom.toValueString());
        if (!this.arguments.isEmpty()) {
            builder.append("(");
            var second = false;
            for (var arg : this.arguments) {
                if (second) builder.append(",");
                builder = arg.append(builder);
                second = true;
            }
            builder.append(")");
        }
        return builder;
    }

    public String key() {
        if (this._key != null) return this._key;
        this._key = this.append(new StringBuilder()).toString();
        return this._key;
    }

    public String predicateIndicator() {
        return this.atom.toValueString()+"/"+this.arity();
    }

    public int arity() {
        return this.arguments.size();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if ((o instanceof String str)) return Objects.equals(this.key(), str);;
        if (!(o instanceof PredicateNode that)) return false;
        return Objects.equals(this.key(), that.key());
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.key());
    }

    public boolean isGoal() {
        return this.arguments.isEmpty() || this.arguments.stream().allMatch(ArgumentNode::isGround);
    }
}
