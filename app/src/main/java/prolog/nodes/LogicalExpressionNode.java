package prolog.nodes;

import prolog.Memory;
import prolog.PrologRuntime;
import prolog.TokenValue;

public class LogicalExpressionNode extends AbstractNode {
    private final ExpressionNode expression;
    private final TokenValue logicalOperation;
    private final ExpressionNode anotherExpression;

    public LogicalExpressionNode(ExpressionNode expression, TokenValue logicalOperation, ExpressionNode anotherExpression) {
        this.expression = expression;
        this.logicalOperation = logicalOperation;
        this.anotherExpression = anotherExpression;
    }
    @Override
    public void execute(PrologRuntime runtime) {

    }

    @Override
    public StringBuilder append(StringBuilder builder) {
        builder = this.expression.append(builder);
        builder.append(this.logicalOperation.toValueString());
        return this.anotherExpression.append(builder);
    }

    public boolean isGround() {
        return this.expression.isGoal() && this.anotherExpression.isGoal();
    }
}
