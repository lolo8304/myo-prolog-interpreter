package prolog.nodes;

import prolog.interpreter.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class ExpressionNode extends AbstractNode implements Term {
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

    private Node node() {
        return this.argument != null ? argument : this.conditionNode != null ? this.conditionNode : this.expression;
    }

    public String key() {
        if (this.argument != null) return this.argument.key();
        if (this.expression != null) return this.expression.key();
        return this.conditionNode.key();
    }

    @Override
    public StringBuilder append(StringBuilder builder) {
        return this.node().append(builder);
    }


    private TermStatus termStatus() {
        if (this.argument != null) return this.argument;
        if (this.expression != null) return this.expression;
        return this.conditionNode;
    }

    @Override
    public boolean isGround() {
        return this.termStatus().isGround();
    }

    @Override
    public boolean isPartiallyInstantiated() {
        return this.termStatus().isPartiallyInstantiated();
    }

    @Override
    public boolean isInstantiated() {
        return this.termStatus().isInstantiated();
    }

    @Override
    public boolean isUnInstantiated() {
        if (this.argument != null) return this.argument.isUnInstantiated();
        return false;
    }

    @Override
    public Term map(Subst s) {
        return this.argument != null ? argument.map(s) : this.conditionNode != null ? this.conditionNode.map(s) : this.expression.map(s);
    }

    @Override
    public Optional<Subst> pmatch(Term term, Subst s) {
        return this.argument != null ? argument.pmatch(term, s) : this.conditionNode != null ? this.conditionNode.pmatch(term, s) : this.expression.pmatch(term, s);
    }

    @Override
    public Optional<Subst> unify(Term y, Subst s) {
        return this.argument != null ? argument.unify(y, s) : this.conditionNode != null ? this.conditionNode.unify(y, s) : this.expression.unify(y, s);
    }

    @Override
    public Optional<Constr> asConstr() {
        return this.argument != null ? argument.asConstr() : this.conditionNode != null ? this.conditionNode.asConstr() : this.expression.asConstr();
    }

    @Override
    public Optional<Var> asVar() {
        return this.argument != null ? argument.asVar() : this.conditionNode != null ? this.conditionNode.asVar() : this.expression.asVar();
    }

    public List<Term> rhs() {
        return this.argument != null ? List.of(this.argument) : this.conditionNode != null ? this.conditionNode.rhs() : this.expression.rhs();
    }


    public Optional<PredicateNode> asPredicate() {
        if (this.argument == null) return Optional.empty();
        return Optional.of(this.argument.asPredicate());
    }

}
