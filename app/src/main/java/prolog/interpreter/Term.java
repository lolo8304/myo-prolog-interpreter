package prolog.interpreter;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

public interface Term {
    public List<Term> EMPTY_LIST = new ArrayList<>();

    StringBuilder append(StringBuilder builder);

    FreeVars freevars();
    Term map(Subst s);

    Optional<Subst> pmatch(Term term, Subst s);

    Optional<Subst> unify(Term y, Subst s);

    Optional<Constr> asConstr();

    Optional<Var> asVar();

    Term asTerm();

}
