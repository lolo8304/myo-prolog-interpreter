package prolog.interpreter;

import prolog.nodes.AbstractNode;
import prolog.nodes.Node;

import java.util.*;

public class Terms extends ArrayList<Term> {
    public final static Terms EMPTY_TERMS = new Terms();
    private FreeVars freevars = new FreeVars();

    public Terms(int initialCapacity) {
        super(initialCapacity);
    }

    public Terms() {
    }
    public Terms(Term... terms) {
        super();
        this.addAll(Arrays.stream(terms).toList());
    }
    public Terms(Term head, Terms tail) {
        super();
        this.add(head);
        this.addAll(tail);
    }

    public Terms(Collection<? extends Term> c) {
        super();
        this.addAll(c);
    }

    @Override
    public boolean add(Term term) {
        this.addFreeVars(term);
        return super.add(term);
    }

    private void addFreeVars(Term term) {
        this.freevars.addAll(term.asTerm().freevars());
    }

    @Override
    public boolean addAll(Collection<? extends Term> c) {
        for (var term: c) {
            this.addFreeVars(term);
        }
        return super.addAll(c);
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

    public FreeVars freevars() {
        return this.freevars;
    }

    public Terms newInstance() {
        var s = this.freevars().asSubs();
        var mappedHead = this.lhs().map(s);
        var mappedTail = new Terms(this.rhs().stream().map(x -> x.map(s)).toList());
        return new Terms(mappedHead, mappedTail);
    }

    public Terms map(Subst s) {
        return new Terms(this.stream().map( t -> t.map(s)).toList());
    }

    public Terms concat(Terms terms) {
        var concatTerms = new Terms(this);
        concatTerms.addAll(terms);
        return concatTerms;
    }

    public Optional<Subst> unify(Terms ys, Subst s) {
        return Constr.unify(this,ys, s);
    }

    public Optional<Subst> pmatch(Terms ys, Subst s) {
        return Constr.pmatch(this,ys, s);
    }

}
