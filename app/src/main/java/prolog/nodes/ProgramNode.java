package prolog.nodes;

import prolog.Memory;
import prolog.PrologRuntime;

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

}
