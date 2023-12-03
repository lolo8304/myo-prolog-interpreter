package prolog.nodes;

import prolog.PrologRuntime;
import prolog.TokenValue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static prolog.nodes.ArgumentNode.NIL_ARGUMENT;

public abstract class CompoundNode extends AbstractNode {
    public final TokenValue functor;
    private String _key;

    public CompoundNode(TokenValue functor) {
        this.functor = functor;
        this._key = null;
    }

    public String principalFunctor() {
        return "("+this.functor.toValueString() +")"+ "/"+this.arity();
    }

    public abstract List<ArgumentNode> arguments();

    public int arity() {
        return this.arguments().size();
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o instanceof String) {
            return Objects.equals(this.key(), o);
        }
        if (!(o instanceof CompoundNode that)) return false;
        return Objects.equals(this.key(), that.key());
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.key());
    }
    @Override
    public void execute(PrologRuntime runtime) {

    }

    public boolean isGround() {
        return !this.isPartiallyInstantiated();
    }

    public boolean isPartiallyInstantiated() {
        return this.arguments().stream().anyMatch(x -> !x.isGround());
    }

    @Override
    public StringBuilder append(StringBuilder builder) {
        builder.append(this.functor.toValueString()).append("(");
        var second = false;
        for (var arg :  this.arguments()) {
            if (second) builder.append(",");
            builder = arg.append(builder);
            second = true;
        }
        builder.append(")");
        return builder;
    }

    public boolean isChars() {
        return false;
    }
    public boolean isListFunctor() {
        return this.functor.value.equals(".");
    }

    public CompoundNode tryAsListNotation() throws IOException {
        if (!this.isListFunctor()) return this;
        if (this.arity() == 1) {
            return new CompoundListNode(
                    this.arguments().get(0),
                    NIL_ARGUMENT);
        } else {
            var args = this.arguments();
            ArgumentNode lastTail = NIL_ARGUMENT;
            for (int i = args.size()-1; i > 0; i--) {
                lastTail = new ArgumentNode(new CompoundListNode(args.get(i), lastTail));
            }
            return lastTail.compoundTerm;
        }
    }
}
