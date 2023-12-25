package prolog.interpreter;

import prolog.nodes.AbstractNode;
import prolog.nodes.Node;

import java.util.*;

public class TermsList extends ArrayList<Term> implements Terms {
    public final static TermsList EMPTY_TERMS = new TermsList();
    private final FreeVars freevars = new FreeVars();

    public TermsList(int initialCapacity) {
        super(initialCapacity);
    }

    public TermsList() {
    }
    public TermsList(Term... terms) {
        super();
        this.addAll(Arrays.stream(terms).toList());
    }
    public TermsList(Term head, TermsList tail) {
        super();
        this.add(head);
        this.addAll(tail);
    }

    public TermsList(Collection<? extends Term> c) {
        super();
        this.addAll(c);
    }

    public TermsList(List<AbstractNode> nodes) {
        super(nodes.stream().map(AbstractNode::asTerm).toList());
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


    @Override
    public String toString() {
        return this.append(new StringBuilder()).toString();
    }

    public Term lhs() {
        return this.get(0);
    }
    public TermsList rhs() {
        return new TermsList(this.subList(1,this.size()));
    }

    @Override
    public StringBuilder append(StringBuilder builder) {
        var second = false;
        for (var term: this) {
            if (second) builder.append(", ");
            second = true;
            builder = term.append(builder);
        }
        return builder;
    }

    public FreeVars freevars() {
        return this.freevars;
    }

    @Override
    public Terms newInstance() {
        var s = this.freevars().asSubs();
        var mappedHead = this.lhs().map(s);
        var mappedTail = new TermsList(this.rhs().stream().map(x -> x.map(s)).toList());
        return new TermsList(mappedHead, mappedTail);
    }

    @Override
    public Terms map(Subst s) {
        return new TermsList(this.stream().map(t -> t.map(s)).toList());
    }


    @Override
    public Optional<Subst> unify(Term y, Subst s) {
        return Optional.empty();
    }

    @Override
    public Optional<Constr> asConstr() {
        return Optional.empty();
    }

    @Override
    public Optional<Var> asVar() {
        return Optional.empty();
    }

    @Override
    public Term asTerm() {
        return null;
    }

    public TermsList concat(Terms terms) {
        var concatTerms = new TermsList(this);
        concatTerms.addAll(terms);
        return concatTerms;
    }

    @Override
    public Optional<Subst> unify(Terms ys, Subst s) {
        return Constr.unify(this,ys, s);
    }

    @Override
    public Optional<Subst> pmatch(Terms ys, Subst s) {
        return Constr.pmatch(this,ys, s);
    }

    @Override
    public Optional<Subst> pmatch(Term term, Subst s) {
        if (term instanceof Terms termAsTerms) return this.pmatch(termAsTerms, s);
        return Optional.empty();
    }

}
