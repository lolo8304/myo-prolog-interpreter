package prolog.interpreter;

import prolog.nodes.ClauseNode;
import prolog.nodes.FactNode;
import prolog.nodes.ProgramNode;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Set;
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

            var clauses = this.top().memory.clauses();
            var s = solve(query, clauses);
            s.forEachOrdered(solution -> {
                System.out.println("solution: "+solution.toString());
            });

        } catch (Exception e) {
            throw new IOException("Error while closing #find solution", e);
        }

    }

    private Stream<Subst> solve(ClauseNode query, Stream<Terms> clauses) {
        var asTerms = query.asTerms();
        return this.solve(asTerms, clauses);
    }

    private Stream<Subst> solve(Terms query, Stream<Terms> clauses) {
        return this.solve1(query, new Subst(), clauses);
    }

    private Stream<Subst> solve1(Terms query, Subst s, Stream<Terms> clauses) {
        if (query.isEmpty()) {
            return Stream.of(s);
        } else {
            var q = query.lhs();
            var query1 = query.rhs();
            final var sresult = new ArrayList<Stream<Subst>>();
            clauses.forEachOrdered(clause -> {
                var s1 = tryClause(clause, q, s);
                s1.forEachOrdered(eachS1 -> {
                    var s2 = solve1(query1, eachS1, clauses);
                    sresult.add(s2);
                });
            });
            return sresult.isEmpty() ? Stream.empty() : sresult.stream().flatMap(x -> x);
        }
    }


    private Stream<Subst> tryClause(Terms clause, Term q, Subst s) {
        var s1 = q.unify(clause.lhs(), s);
        return s1.map(bindings -> this.solve1(clause.rhs(), bindings, this.top().memory.clauses())).orElseGet(Stream::empty);
    }

//    private void solve() {
//        if (query.isGround()) {
//            var facts = ;
//            if (facts != null) {
//                solveFacts(query, facts);
//
//            } else {
//                if (Prolog.verbose()) System.out.println("Fact wrong: "+query);
//                var binding = new Binding(TokenValue.FALSE.toValueString(), query);
//                this.top().addSolution(binding);
//            }
//        }
//    }

    private static void solveFacts(FactNode query, Set<FactNode> facts) {
        for (var fact:  facts) {
            var s = query.freevars().asSubs();
            var newS = query.unify(fact, s);
            if (newS.isPresent()) {
                var t = fact.map(newS.get());
            }
        }
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
