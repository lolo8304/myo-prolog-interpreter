package prolog.nodes;

import prolog.TokenValue;
import prolog.interpreter.Constr;
import prolog.interpreter.FreeVars;
import prolog.interpreter.PrologRuntime;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ProgramNode extends AbstractNode {
    public final List<ClauseNode> clauses;

    public ProgramNode() {
        this.clauses = new ArrayList<>();
    }

    public ProgramNode(ClauseNode clause) {
        this();
        this.clauses.add(clause);
    }

    public ProgramNode addClause(ClauseNode clause) {
        this.clauses.add(clause);
        return this;
    }
    @Override
    public void execute(PrologRuntime runtime) throws IOException {
        for (ClauseNode x : this.clauses) {
            x.execute(runtime);
        }
    }

    @Override
    public StringBuilder append(StringBuilder builder) {
        for (int i = 0; i < this.clauses.size(); i++) {
            this.clauses.get(i).append(builder);;
            builder.append(".");
        }
        return builder;
    }

    @Override
    public FreeVars freevars() {
        return new FreeVars(this.clauses.stream().flatMap(x -> x.freevars().stream()).toList());
    }

    @Override
    public boolean isGround() {
        return this.clauses.stream().allMatch(ClauseNode::isGround);
    }

    @Override
    public boolean isPartiallyInstantiated() {
        return this.clauses.stream().allMatch(ClauseNode::isPartiallyInstantiated);
    }

    @Override
    public boolean isInstantiated() {
        return this.clauses.stream().allMatch(ClauseNode::isInstantiated);
    }

    @Override
    public boolean isUnInstantiated() {
        return this.clauses.stream().allMatch(ClauseNode::isUnInstantiated);
    }

    @Override
    public Optional<Constr> asConstr() {
        return Optional.empty();
    }
}
