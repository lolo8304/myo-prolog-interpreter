package prolog.nodes;

import prolog.interpreter.*;
import prolog.TokenValue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class LogicalExpressionNode extends ConditionNode {
    public LogicalExpressionNode(List<ArgumentNode> terms, List<TokenValue> conditions) {
        super(terms, conditions);
    }

    public Terms asTerms() {
        return new TermsList(this.asConstr().orElseThrow().terms);
    }

}
