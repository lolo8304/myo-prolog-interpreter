package prolog.nodes;

import prolog.interpreter.*;
import prolog.TokenValue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static prolog.nodes.ArgumentNode.NIL_ARGUMENT;

public abstract class CompoundNode extends AbstractNode implements Term {
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
    public boolean isGround() {
        return !this.isPartiallyInstantiated();
    }

    @Override
    public boolean isPartiallyInstantiated() {
        return this.arguments().stream().anyMatch(ArgumentNode::isUnInstantiated);
    }

    @Override
    public boolean isInstantiated() {
        return this.arguments().stream().allMatch(ArgumentNode::isInstantiated);
    }

    @Override
    public boolean isUnInstantiated() {
        return false;
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
            return asListNotation(this.arguments());
        }
    }

    public static CompoundListNode asListNotation(List<ArgumentNode> argumentNodes) {
        if (argumentNodes.isEmpty()) {
            throw new RuntimeException("List notation conversation cannot be empty");
        }
        var startIndex = argumentNodes.size()-1;
        ArgumentNode lastTail = argumentNodes.get(startIndex);
        if (lastTail.equals(NIL_ARGUMENT)) {
            startIndex--;
        } else {
            lastTail = NIL_ARGUMENT;
        }
        for (int i = startIndex; i >= 0; i--) {
            lastTail = new ArgumentNode(new CompoundListNode(argumentNodes.get(i), lastTail));
        }
        return (CompoundListNode)lastTail.compoundTerm;
    }

    @Override
    public FreeVars freevars() {
        return new FreeVars(this.arguments().stream().flatMap(x -> x.freevars().stream()).toList());
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
        List<Term> terms = new ArrayList<>(this.arguments());
        return Optional.of(new Constr(this.functor, terms));
    }

    @Override
    public Optional<Var> asVar() {
        return Optional.empty();
    }

    public PredicateNode asPredicate() {
        return new PredicateNode(this.functor, this.arguments());
    }


}
