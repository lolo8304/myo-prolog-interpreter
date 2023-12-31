package prolog.nodes;

import prolog.Token;
import prolog.TokenValue;
import prolog.interpreter.*;

import java.io.IOException;
import java.util.*;
import java.util.function.Consumer;

public class CompoundListNode extends CompoundNode implements Iterable<ArgumentNode> {

    private final ArgumentNode head;
    private final ArgumentNode tail;

    @Override
    public List<ArgumentNode> arguments() {
        var list = new ArrayList<ArgumentNode>();
        list.add(this.head);
        list.addAll(this.tail.arguments());
        return list;
    }

    public CompoundListNode(ArgumentNode head, ArgumentNode tail) {
        super(new TokenValue(Token.QUOTED_ATOM, "."));
        this.head = head;
        this.tail = tail;
    }

    public boolean isChars() {
        // true if head = atom with 1 char
        // all tails is Chars
        // head with [] is not valid char list
        return this.head != ArgumentNode.NIL_ARGUMENT
                && this.head.isChars() && this.tail.isChars();
    }

    @Override
    public StringBuilder append(StringBuilder builder) {
        if (this.isChars()) {
            builder.append('"');
            var iterator = this.iterator();
            while (iterator.hasNext()) {
                iterator.next().append(builder);
            }
            builder.append('"');
            return builder;
        } else if (this.hasTailWithVariable()) {
            builder.append("[");
            this.head.append(builder);
            builder.append("|");
            this.tail.append(builder);
            builder.append("]");
            return builder;
        } else {
            return super.append(builder);
        }
    }

    private boolean hasTailWithVariable() {
        return this.tail.asVar().isPresent();
    }

    @Override
    public Iterator<ArgumentNode> iterator() {
        return new ListIterator(this);
    }

    private static class ListIterator implements Iterator<ArgumentNode> {

        private CompoundListNode list;

        public ListIterator(CompoundListNode list) {
            this.list = list;
        }
        @Override
        public boolean hasNext() {
            return list != null;
        }

        @Override
        public ArgumentNode next() {
            if (list != null) {
                var tail = this.list.tail;
                var head = this.list.head;
                if (tail == ArgumentNode.NIL_ARGUMENT) {
                    this.list = null;
                } else if (tail.compoundTerm != null && tail.compoundTerm.isListFunctor()) {
                    this.list = (CompoundListNode) tail.compoundTerm;
                } else {
                    throw new IllegalArgumentException("next is not a list element '" + tail + "'");
                }
                return head;
            } else {
                throw new IllegalArgumentException("cannot iterate over last tail in list");
            }
        }
    }

    @Override
    public Optional<Constr> asConstr() {
        return Optional.of(new ListConstr(this));
    }

    @Override
    public Terms asTerms() {
        var termTail = this.tail.asTerm();
        if (termTail instanceof Var tailAsVar) {
            return new TermsList(this.head, new TermsVar(tailAsVar));
        } else {
            return new TermsList(this.head, termTail);
        }
    }

    @Override
    public PredicateNode asPredicate() {
        return new PredicateNode(this.functor, this.arguments());
    }
}
