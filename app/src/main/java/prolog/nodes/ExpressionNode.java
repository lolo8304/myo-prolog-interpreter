package prolog.nodes;

import prolog.Memory;
import prolog.PrologRuntime;

public class ExpressionNode extends AbstractNode {
    private final ArgumentNode argument;
    private final ConditionNode conditionNode;
    private final LogicalExpressionNode expression;

    public ExpressionNode(ArgumentNode argument) {
        this.argument = argument;
        this.conditionNode = null;
        this.expression = null;
    }

    public ExpressionNode(ConditionNode conditionNode) {
        this.argument = null;
        this.conditionNode = conditionNode;
        this.expression = null;
    }
    public ExpressionNode(LogicalExpressionNode logicalExpression) {
        this.argument = null;
        this.conditionNode = null;
        this.expression = logicalExpression;
    }
    @Override
    public void execute(PrologRuntime runtime) {
    }

    private Node node() {
        return this.argument != null ? argument : this.conditionNode != null ? this.conditionNode : this.expression;
    }

    @Override
    public StringBuilder append(StringBuilder builder) {
        return this.node().append(builder);
    }

    public boolean isGoal() {
        return this.argument != null ? this.argument.isGround() : (this.expression != null && this.expression.isGround());
    }
}
