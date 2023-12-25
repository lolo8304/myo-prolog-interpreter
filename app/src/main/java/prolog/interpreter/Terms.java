package prolog.interpreter;

import prolog.nodes.AbstractNode;
import prolog.nodes.Node;

import java.util.*;

public interface Terms extends Term, Node, List<Term> {
    public final static Terms EMPTY_TERMS = new TermsList();

    boolean add(Term term);

    boolean addAll(Collection<? extends Term> c);

    Term lhs();
    Terms rhs();

    Terms newInstance();

    Terms map(Subst s);

    Terms concat(Terms terms);

    Optional<Subst> unify(Terms ys, Subst s);

    Optional<Subst> pmatch(Terms ys, Subst s);

}
