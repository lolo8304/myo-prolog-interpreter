package prolog.nodes;

import prolog.Memory;
import prolog.PrologRuntime;
import prolog.TokenValue;

public class ConditionNode extends AbstractNode {

    private final ArgumentNode term;
    private final TokenValue condition;
    private final ArgumentNode otherTerm;


    public ConditionNode(ArgumentNode term, TokenValue condition, ArgumentNode otherTerm) {

        this.term = term;
        this.condition = condition;
        this.otherTerm = otherTerm;
    }
    @Override
    public void execute(PrologRuntime runtime) {

    }

    @Override
    public StringBuilder append(StringBuilder builder) {
        builder = this.term.append(builder);
        builder.append(this.condition.toValueString());
        return this.otherTerm.append(builder);
    }

}
