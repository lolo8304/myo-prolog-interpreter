package prolog.nodes;

import prolog.interpreter.*;
import prolog.TokenValue;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

public class PredicateNode extends AbstractNode implements Term {
    public final TokenValue atom;
    public final List<ArgumentNode> arguments;

    private String _key;

    public PredicateNode(TokenValue atom) {
        this.atom = atom;
        this.arguments = new ArrayList<>();
        this._key = null;
    }
    public PredicateNode(TokenValue atom, List<ArgumentNode> arguments) {
        this.atom = atom;
        this.arguments = arguments;
        this._key = null;
    }

    public PredicateNode addArgument(ArgumentNode argument) {
        this.arguments.add(argument);
        this._key = null;
        return this;
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
        if ((o instanceof String str)) return Objects.equals(this.key(), str);
        if (!(o instanceof PredicateNode that)) return false;
        return Objects.equals(this.key(), that.key());
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.key());
    }


    // true if: no arguments and all of the arguments are Ground
    @Override
    public boolean isGround() {
        if (this.arguments.isEmpty()) {
            return this.atom.isGround();
        }
        return this.arguments.stream().allMatch(ArgumentNode::isGround);
    }


    // at least 1 must be Uninstantiated
    @Override
    public boolean isPartiallyInstantiated() {
        if (this.arguments.isEmpty()) {
            return this.atom.isPartiallyInstantiated();
        }
        return this.arguments.stream().anyMatch(x ->
            x.isUnInstantiated() || x.isPartiallyInstantiated()
        );
    }

    @Override
    public boolean isInstantiated() {
        if (this.arguments.isEmpty()) {
            return this.atom.isInstantiated();
        }
        return this.arguments.stream().allMatch(ArgumentNode::isInstantiated);
    }

    @Override
    public boolean isUnInstantiated() {
        if (this.arguments.isEmpty()) {
            return this.atom.isUnInstantiated();
        }
        return false;
    }

    @Override
    public FreeVars freevars() {
        if (this.arity() == 0) {
            return TokenValue.EMPTY_LIST;
        } else {
            return new FreeVars(this.arguments.stream().flatMap(x -> x.freevars().stream()).toList());
        }
    }

    @Override
    public Term map(Subst s) {
        return new Constr(this.atom, this.arguments.stream().map(x -> x.map(s)).toList());
    }

    @Override
    public Optional<Constr> asConstr() {
        if (this.atom.toValueString().equals(".")) {
            return Optional.of(new ListConstr(CompoundListNode.asListNotation(this.arguments)));
        } else {
            return Optional.of(new Constr(this.atom, new ArrayList<Term>(this.arguments.stream().map(ArgumentNode::asTerm).toList())));
        }
    }

    @Override
    public Optional<Var> asVar() {
        return Optional.empty();
    }

    @Override
    public Terms concat(Term term) {
        return new TermsList(this.asTerm(), term);
    }

    @Override
    public Optional<Subst> pmatch(Term term, Subst s) {
        return this.asConstr().get().pmatch(term, s);
    }

    @Override
    public Optional<Subst> unify(Term y, Subst s) {
        return this.asConstr().get().unify(y,s);
    }
}
