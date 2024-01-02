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
        return this.terms.get(0).asTerm();
    }

    @Override
    public Term rhs() {
        var tail = this.terms.get(1);
        if (tail instanceof Terms tailAsTerms) {
            return tailAsTerms;
        } else if (tail.equals(TokenValue.NIL)) {
            return tail;
        } else if (tail instanceof ListConstr tailAsListConstr) {
            return tailAsListConstr;
        } else if (tail instanceof Var tailAsVar) {
            return new TermsVar(tailAsVar);
        } else if (tail instanceof TokenValue tailAsTokenValue && tailAsTokenValue.is(Token.VARIABLE)) {
            return new TermsVar(tailAsTokenValue);
        } else {
            throw new RuntimeException("Lists should have always a list in rhs");
            //return new TermsList(tail);
        }
    }

    @Override
    public Terms concat(Term term) {
        return new TermsList(this, term);
    }

    @Override
    public Optional<Subst> unify(Term y, Subst s) {
        var yAsVar = y.asVar();
        if (yAsVar.isPresent()) {
            var term = s.lookup(yAsVar.get().name());
            if (term.isEmpty()) {
                return Optional.of(new Subst(new Binding(yAsVar.get().name(), this), s));
            } else {
                return term.get().unify(yAsVar.get(), s);
            }
        }
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
                var newS = headX.unify(headY, s);
                if (newS.isPresent()) {
                    var tailX = this.rhs();
                    var tailY = yAsListConstr.rhs();
                    return tailX.unify(tailY, newS.get());
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
        //builder.append(this.atom.toValueString());
        if (this.terms.size() == 0) return builder;

        builder.append("[");

        var head = this.lhs();
        head.append(builder);

        builder.append(" | ");
        var tail = this.rhs();
        tail.append(builder);

        builder.append(" ]");
        return builder;
    }
}
