package prolog.interpreter;

import prolog.Prolog;
import prolog.nodes.ClauseNode;
import prolog.nodes.ProgramNode;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class PrologRuntime {

    public Context top;
    private boolean inConsultingMode = false;

    public PrologRuntime() {
        this.top = new Context(this);
    }

    public void execute(ProgramNode program) throws IOException {
        program.execute(this);
    }
    public void consult(ProgramNode program) throws IOException {
        this.inConsultingMode = true;
        try {
            program.execute(this);
        } finally {
            this.inConsultingMode = false;
        }
    }

    public Memory memory() {
        return this.top.memory;
    }

    public void findSolution(ClauseNode query) throws IOException {
        try (var context = this.start())  {

            var clauses = this.top().memory.clauses().collect(Collectors.toList());
            var s = solve(query, clauses);
            s.forEach(solution -> {
                System.out.println("solution: "+solution.toString());
            });

        } catch (Exception e) {
            throw new IOException("Error while closing #find solution", e);
        }

    }

    private List<Subst> solve(ClauseNode query, List<Terms> clauses) {
        var asTerms = query.asTerms();
        return this.solve(asTerms, clauses);
    }

    private List<Subst> solve(Terms query, List<Terms> clauses) {
        return this.solve1(query, new Subst(), clauses);
    }

    private List<Subst> solve1(Terms query, Subst s, List<Terms> clauses) {
        if (query.isEmpty()) {
            return new ArrayList<>(Collections.singletonList(s));
        } else {
            final var sresult = new ArrayList<Subst>();
            clauses.forEach(clause -> {
                Terms newClause = clause.newInstance();
                var s1 = tryClause(newClause, query, s, clauses);
                if (!s1.isEmpty()) {
                    sresult.addAll(s1);
                }
            });
            return sresult;
        }
    }


    private List<Subst> tryClause(Terms clause, Terms query, Subst s, List<Terms> clauses) {
        var clauseHead = clause.lhs().map(s);
        var queryHead = query.lhs();
        var queryTail = query.rhs();

        if (queryHead.pmatch(clauseHead, s).isPresent()) {
            var newS = queryHead.unify(clauseHead, s);
            if (newS.isPresent()) {
                if (Prolog.verbose()) {
                    System.out.println("UNIFIED: queryHead = "+queryHead);
                    System.out.println("        clauseHead = "+clauseHead);
                    System.out.println("             subst = "+newS.get());
                }
                // Construct new query with the body of the clause and remaining goals
                var clauseBody = clause.rhs().map(newS.get());
                var newQuery = clauseBody.concat(queryTail);
                return this.solve1(newQuery, newS.get(), clauses);
            }
        }
        return new ArrayList<>();
    }

    public Context top() {
        return this.top.top();
    }

    public Context current() {
        return this.top;
    }

    public Context start() {
        this.top = new Context(this.top, this);
        return this.top;
    }
    public Context stop() throws IOException {
        if (this.top.parent == null) {
            throw new IOException("Top context cannot be stopped.");
        }
        this.top = this.top.parent;
        return this.top;
    }


    public boolean inConsultingMode() {
        return this.inConsultingMode;
    }
}
