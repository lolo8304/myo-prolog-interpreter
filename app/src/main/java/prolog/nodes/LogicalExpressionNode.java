package prolog.nodes;

import prolog.interpreter.*;
import prolog.Token;
import prolog.TokenValue;

import java.util.ArrayList;
import java.util.List;

public class LogicalExpressionNode extends ConditionNode {
    public LogicalExpressionNode(List<ArgumentNode> terms, List<TokenValue> conditions) {
        super(terms, conditions);
    }

    public Terms asTerms() {
        return this.rhs();
    }

    @Override
    public Terms rhs() {
        var goals = new TermsList();
        var start = 0;
        for (int i = 0; i < this.conditions.size(); i++) {
            if (this.conditions.get(i).is(Token.COMMA)) {
                goals.add(this.goal(start, i));
                start = i + 1;
            }
        }
        goals.add(this.goal(start, this.conditions.size()));
        return goals;
    }

    private Term goal(int startTermIndex, int endConditionIndex) {
        if (startTermIndex == endConditionIndex) {
            return this.terms.get(startTermIndex).asTerm();
        }

        var firstCondition = this.conditions.get(startTermIndex);
        if ((firstCondition.is(Token.ARITHMETIC_UNIFY_BINARY_OPERATOR) || firstCondition.isComparisonOperator())
                && endConditionIndex > startTermIndex + 1) {
            return new Constr(
                    firstCondition,
                    List.of(
                            this.terms.get(startTermIndex).asTerm(),
                            this.infixTerm(startTermIndex + 1, endConditionIndex)
                    ));
        }

        return new Constr(firstCondition, this.termSlice(startTermIndex, endConditionIndex + 1));
    }

    private Term infixTerm(int startTermIndex, int endConditionIndex) {
        var result = this.terms.get(startTermIndex).asTerm();
        for (int i = startTermIndex; i < endConditionIndex; i++) {
            result = new Constr(
                    this.conditions.get(i),
                    List.of(result, this.terms.get(i + 1).asTerm()));
        }
        return result;
    }

    private List<Term> termSlice(int startInclusive, int endExclusive) {
        var slice = new ArrayList<Term>();
        for (int i = startInclusive; i < endExclusive; i++) {
            slice.add(this.terms.get(i).asTerm());
        }
        return slice;
    }

}
