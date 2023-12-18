package prolog.interpreter;

import prolog.Token;
import prolog.TokenValue;
import prolog.nodes.CompoundListNode;
import prolog.nodes.PredicateNode;

import java.util.List;
import java.util.Optional;

public class ListConstr extends Constr {
    public ListConstr(CompoundListNode list) {
        super(list.functor, list.asTerms());
    }

    public ListConstr(TokenValue nil) {
        super(nil);
    }

    public ListConstr(TokenValue atom, List<Term> terms) {
        super(atom, terms);
    }

    @Override
    public Term lhs() {
        return this.terms.get(0);
    }

    @Override
    public Terms rhs() {
        return (Terms)this.terms.get(1);
    }

    @Override
    public Optional<Subst> unify(Term y, Subst s) {
        var yAsVar = y.asVar();
        if (yAsVar.isPresent()) return yAsVar.get().unify(this, s);
        var yAsConstr = y.asConstr();
        if (yAsConstr.isEmpty()) return Optional.empty();

        if (yAsConstr.get() instanceof ListConstr yAsListConstr) {
            // both are list Const
            if (this.atom.is(Token.nil) && yAsListConstr.atom.is(Token.nil)) {
                return Optional.of(s);
            } else if (this.atom.is(Token.nil) || yAsListConstr.atom.is(Token.nil)) {
                return Optional.empty();
            } else {
                /*
                    else if both have head and tail:
                        new_subst = unify(head of term1, head of term2, substitution)
                        if new_subst is not failure:
                            return unify(tail of term1, tail of term2, new_subst)
                        else:
                            return failure
                 */
                var headX = this.lhs();
                var headY = yAsListConstr.lhs();
                var newS = headX.unify(headY,s);
                if (newS.isPresent()) {
                    var tailX = this.rhs();
                    var tailY = yAsListConstr.rhs();
                    return tailX.unify(tailY,s);
                } else {
                    return Optional.empty();
                }
            }
        } else {
            return Optional.empty();
        }
    }


    @Override
    public Term map(Subst s) {
        return new ListConstr(this.atom, this.terms.stream().map( x -> x.map(s)).toList());
    }

    @Override
    public StringBuilder append(StringBuilder builder) {
        if (this.atom.is(Token.nil)) {
            builder.append("[]");
            return builder;
        }
        builder.append(this.atom.toValueString());
        if (this.terms.size() == 0) return builder;

        builder.append("[");
        var second = false;
        for (var term: this.terms) {
            if (second) {
                builder.append(" | ");
            }
            builder = term.append(builder);
            if (this.freevars().stream().anyMatch(x -> x.toValueString().equals(term.toString()))) {
                builder.append("*");
            }
            second=true;
        }
        builder.append("]");
        return builder;
    }
}
