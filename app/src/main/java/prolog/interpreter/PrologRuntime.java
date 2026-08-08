package prolog.interpreter;

import prolog.Prolog;
import prolog.Lexer;
import prolog.Parser;
import prolog.Token;
import prolog.TokenValue;
import prolog.nodes.ClauseNode;
import prolog.nodes.ProgramNode;

import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.function.Supplier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class PrologRuntime {

    public Context top;
    private boolean inConsultingMode = false;
    private Supplier<Character> solutionContinuationReader;

    public PrologRuntime() {
        this.top = new Context(this);
        this.solutionContinuationReader = this::readSolutionContinuation;
    }

    public void setSolutionContinuationReader(Supplier<Character> solutionContinuationReader) {
        this.solutionContinuationReader = solutionContinuationReader;
    }

    public void useDefaultSolutionContinuationReader() {
        this.solutionContinuationReader = this::readSolutionContinuation;
    }

    public void execute(ProgramNode program) throws IOException {
        program.execute(this);
    }

    public void consult(ProgramNode program) throws IOException {
        program.consult(this);
    }

    public void consult(Reader reader) throws IOException {
        var lexer = new Lexer(reader);
        var parser = new Parser(lexer);
        var program = parser.parse();
        while (program.isPresent()) {
            this.consult(program.get());
            program = parser.parse();
        }
    }

    public Memory memory() {
        return this.top.memory;
    }

    public void findSolution(ClauseNode query) throws IOException {
        try (var context = this.start())  {

            var clauses = this.top().memory.clauses().collect(Collectors.toList());
            var s = solve(query, clauses);
            this.printSolutions(query, s);

        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException("Error while closing #find solution", e);
        }

    }

    private void printSolutions(ClauseNode query, List<Subst> solutions) {
        if (solutions.isEmpty()) {
            System.out.println("false.");
            return;
        }

        for (int i = 0; i < solutions.size(); i++) {
            var solution = solutions.get(i).toString(query.freevars());
            if (solution.isEmpty()) {
                System.out.println("true.");
            } else {
                System.out.println("solution: " + solution);
            }

            var continuation = this.solutionContinuationReader.get();
            if (continuation != ';') {
                return;
            }
            if (i == solutions.size() - 1) {
                System.out.println("false.");
                return;
            }
        }
    }

    private char readSolutionContinuation() {
        try {
            int ch;
            do {
                ch = System.in.read();
            } while (ch >= 0 && Character.isWhitespace(ch));
            if (ch == ';') {
                return ';';
            }
            return '.';
        } catch (IOException e) {
            return '.';
        }
    }

    private List<Subst> solve(ClauseNode query, List<Terms> clauses) throws IOException {
        var asTerms = query.asTerms();
        return this.solve(asTerms, clauses);
    }

    private List<Subst> solve(Terms query, List<Terms> clauses) throws IOException {
        return this.solve1(query, new Subst(), clauses);
    }

    private List<Subst> solve1(Terms query, Subst s, List<Terms> clauses) throws IOException {
        if (query.isEmpty()) {
            return new ArrayList<>(Collections.singletonList(s));
        } else {
            var builtinSolutions = this.tryBuiltin(query, s, clauses);
            if (builtinSolutions.isPresent()) {
                return builtinSolutions.get();
            }

            final var sresult = new ArrayList<Subst>();
            for (var clause: clauses) {
                Terms newClause = clause.newInstance();
                var s1 = tryClause(newClause, query, s, clauses);
                if (!s1.isEmpty()) {
                    sresult.addAll(s1);
                }
            }
            return sresult;
        }
    }


    private List<Subst> tryClause(Terms clause, Terms query, Subst s, List<Terms> clauses) throws IOException {
        var clauseHead = clause.lhs().map(s);
        var queryHead = query.lhs();
        var queryTail = (Terms)query.rhs();

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

    private Optional<List<Subst>> tryBuiltin(Terms query, Subst s, List<Terms> clauses) throws IOException {
        var queryHead = query.lhs().map(s);
        var queryTail = (Terms)query.rhs();
        var queryHeadConstr = queryHead.asConstr();
        if (queryHeadConstr.isEmpty()) {
            return Optional.empty();
        }

        var goal = queryHeadConstr.get();
        var asserted = this.tryAssert(goal, s, queryTail, clauses);
        if (asserted.isPresent()) {
            return asserted;
        }

        var consulted = this.tryConsult(goal, s, queryTail);
        if (consulted.isPresent()) {
            return consulted;
        }

        if (goal.atom.is(Token.ARITHMETIC_UNIFY_BINARY_OPERATOR) && goal.atom.toValueString().equals("is")) {
            if (goal.terms.size() != 2) {
                return Optional.of(Collections.emptyList());
            }

            var value = this.evaluateNumber(goal.terms.get(1), s);
            if (value.isEmpty()) {
                return Optional.of(Collections.emptyList());
            }

            var numberTerm = new TokenValue(Token.NUMBER, value.get());
            var newS = goal.terms.get(0).unify(numberTerm, s);
            if (newS.isEmpty()) {
                return Optional.of(Collections.emptyList());
            }
            return Optional.of(this.solve1(queryTail, newS.get(), clauses));
        }

        if (this.isComparisonBuiltin(goal)) {
            if (goal.terms.size() != 2) {
                return Optional.of(Collections.emptyList());
            }

            var comparison = this.evaluateComparison(goal, s);
            if (comparison.isPresent()) {
                return Optional.of(this.solve1(queryTail, comparison.get(), clauses));
            }
            if (!queryTail.isEmpty() && this.hasUnboundVariables(goal, s)) {
                return Optional.of(this.solve1(queryTail.concat(goal), s, clauses));
            }
            return Optional.of(Collections.emptyList());
        }

        return Optional.empty();
    }

    private Optional<List<Subst>> tryAssert(Constr goal, Subst s, Terms queryTail, List<Terms> clauses) throws IOException {
        var name = goal.atom.toValueString();
        if (!name.equals("asserta") && !name.equals("assertz")) {
            return Optional.empty();
        }
        if (goal.terms.size() != 1) {
            return Optional.of(Collections.emptyList());
        }

        var clause = this.assertedClause(goal.terms.get(0).map(s));
        if (clause.isEmpty()) {
            return Optional.of(Collections.emptyList());
        }

        this.top().memory.assertClause(clause.get(), name.equals("asserta"));
        var nextClauses = this.top().memory.clauses().collect(Collectors.toList());
        return Optional.of(this.solve1(queryTail, s, nextClauses));
    }

    private Optional<List<Subst>> tryConsult(Constr goal, Subst s, Terms queryTail) throws IOException {
        if (!goal.atom.toValueString().equals("consult")) {
            return Optional.empty();
        }
        if (goal.terms.size() != 1) {
            return Optional.of(Collections.emptyList());
        }

        var fileName = this.consultFileName(goal.terms.get(0).map(s));
        if (fileName.isEmpty()) {
            return Optional.of(Collections.emptyList());
        }

        try (var reader = new FileReader(fileName.get())) {
            this.consult(reader);
        }
        var nextClauses = this.top().memory.clauses().collect(Collectors.toList());
        return Optional.of(this.solve1(queryTail, s, nextClauses));
    }

    private Optional<String> consultFileName(Term term) {
        var fileTerm = term.asTerm().asConstr();
        if (fileTerm.isEmpty()) {
            return Optional.empty();
        }

        var file = fileTerm.get();
        if (!file.terms.isEmpty()) {
            return Optional.empty();
        }
        if (!file.atom.is(Token.ATOM, Token.QUOTED_ATOM)) {
            return Optional.empty();
        }
        return Optional.of(file.atom.toValueString());
    }

    private Optional<Terms> assertedClause(Term term) {
        var asserted = term.asConstr();
        if (asserted.isEmpty()) {
            return Optional.empty();
        }

        var clause = asserted.get();
        if (clause.atom.is(Token.UNIFY) && clause.atom.toValueString().equals(":-")) {
            if (clause.terms.size() != 2) {
                return Optional.empty();
            }
            var head = clause.terms.get(0).asConstr();
            if (head.isEmpty()) {
                return Optional.empty();
            }
            return Optional.of(new TermsList(head.get()).concat(assertedRuleBody(clause.terms.get(1))));
        }

        if (!clause.freevars().isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new TermsList(clause));
    }

    private Terms assertedRuleBody(Term body) {
        var bodyConstr = body.asConstr();
        if (bodyConstr.isPresent() && bodyConstr.get().atom.is(Token.COMMA)) {
            return bodyConstr.get().terms;
        }
        return new TermsList(body);
    }

    private boolean isComparisonBuiltin(Constr goal) {
        return goal.atom.is(
                Token.BINARY_COMPARISON_OPERATOR,
                Token.ARITHMETIC_EQUALITY_BINARY_OPERATOR,
                Token.ARITHMETIC_INEQUALITY_BINARY_OPERATOR);
    }

    private Optional<Subst> evaluateComparison(Constr goal, Subst s) {
        var operator = goal.atom.toValueString();
        var left = goal.terms.get(0);
        var right = goal.terms.get(1);

        if (operator.equals("=")) {
            return left.unify(right, s);
        }

        if (operator.equals("\\=")) {
            return left.unify(right, s).isEmpty() ? Optional.of(s) : Optional.empty();
        }

        if (operator.equals("#=")) {
            return this.evaluateArithmeticEquality(left, right, s);
        }

        var leftValue = this.evaluateNumber(left, s);
        var rightValue = this.evaluateNumber(right, s);
        if (leftValue.isEmpty() || rightValue.isEmpty()) {
            return Optional.empty();
        }

        var comparison = Double.compare(leftValue.get().doubleValue(), rightValue.get().doubleValue());
        var matches = switch (operator) {
            case ">", "#>" -> comparison > 0;
            case ">=", "#>=" -> comparison >= 0;
            case "<", "#<" -> comparison < 0;
            case "=<", "#=<" -> comparison <= 0;
            case "=:=" -> comparison == 0;
            case "=\\=", "#\\=" -> comparison != 0;
            default -> false;
        };
        return matches ? Optional.of(s) : Optional.empty();
    }

    private Optional<Subst> evaluateArithmeticEquality(Term left, Term right, Subst s) {
        var leftValue = this.evaluateNumber(left, s);
        var rightValue = this.evaluateNumber(right, s);

        if (leftValue.isPresent() && rightValue.isPresent()) {
            return Double.compare(leftValue.get().doubleValue(), rightValue.get().doubleValue()) == 0
                    ? Optional.of(s)
                    : Optional.empty();
        }

        if (leftValue.isEmpty() && rightValue.isPresent()) {
            return left.unify(new TokenValue(Token.NUMBER, rightValue.get()), s);
        }

        if (leftValue.isPresent()) {
            return right.unify(new TokenValue(Token.NUMBER, leftValue.get()), s);
        }

        return Optional.empty();
    }

    private boolean hasUnboundVariables(Term term, Subst s) {
        return !term.map(s).freevars().isEmpty();
    }

    private Optional<Number> evaluateNumber(Term term, Subst s) {
        var mappedTerm = term.map(s);
        var constr = mappedTerm.asConstr();
        if (constr.isEmpty()) {
            return Optional.empty();
        }

        var value = constr.get();
        if (value.atom.is(Token.NUMBER) && value.terms.isEmpty()) {
            return Optional.of((Number)value.atom.value);
        }

        var operator = value.atom.toValueString();
        if (value.terms.size() == 1) {
            var operand = this.evaluateNumber(value.terms.get(0), s);
            if (operand.isEmpty()) {
                return Optional.empty();
            }
            return switch (operator) {
                case "+" -> Optional.of(operand.get());
                case "-" -> Optional.of(this.normalizeNumber(-operand.get().doubleValue()));
                default -> Optional.empty();
            };
        }

        if (value.terms.size() != 2) {
            return Optional.empty();
        }

        var left = this.evaluateNumber(value.terms.get(0), s);
        var right = this.evaluateNumber(value.terms.get(1), s);
        if (left.isEmpty() || right.isEmpty()) {
            return Optional.empty();
        }

        var leftValue = left.get().doubleValue();
        var rightValue = right.get().doubleValue();
        return switch (operator) {
            case "+" -> Optional.of(this.normalizeNumber(leftValue + rightValue));
            case "-" -> Optional.of(this.normalizeNumber(leftValue - rightValue));
            case "*" -> Optional.of(this.normalizeNumber(leftValue * rightValue));
            case "/" -> Optional.of(leftValue / rightValue);
            case "//" -> Optional.of((long)(leftValue / rightValue));
            default -> Optional.empty();
        };
    }

    private Number normalizeNumber(double value) {
        if (value == Math.rint(value)) {
            return (long)value;
        }
        return value;
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

    public void consultingModeOn() {
        this.inConsultingMode = true;
    }
    public void consultingModeOff() {
        this.inConsultingMode = false;
    }
}
