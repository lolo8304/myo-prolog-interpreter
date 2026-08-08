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
import java.util.Comparator;
import java.util.function.Supplier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class PrologRuntime {

    public Context top;
    private boolean inConsultingMode = false;
    private Supplier<Character> solutionContinuationReader;
    private final Map<String, IsoPrologBuiltInPredicate> isoPrologBuiltIns;
    private final Map<String, String> prologFlags;
    private final List<Constr> operatorDeclarations;
    private final Map<String, String> charConversions;

    public PrologRuntime() {
        this.top = new Context(this);
        this.solutionContinuationReader = this::readSolutionContinuation;
        this.isoPrologBuiltIns = new LinkedHashMap<>();
        this.prologFlags = new LinkedHashMap<>();
        this.operatorDeclarations = new ArrayList<>();
        this.charConversions = new LinkedHashMap<>();
        this.registerDefaultFlags();
        this.registerDefaultOperators();
        this.registerIsoPrologBuiltIns();
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
            var isoPrologBuiltInSolutions = this.tryIsoPrologBuiltIn(query, s, clauses);
            if (isoPrologBuiltInSolutions.isPresent()) {
                return isoPrologBuiltInSolutions.get();
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

    private Optional<List<Subst>> tryIsoPrologBuiltIn(Terms query, Subst s, List<Terms> clauses) throws IOException {
        var queryHead = query.lhs().map(s);
        var queryTail = (Terms)query.rhs();
        var queryHeadConstr = queryHead.asTerm().asConstr();
        if (queryHeadConstr.isEmpty()) {
            return Optional.empty();
        }

        var goal = queryHeadConstr.get();
        var isoPrologBuiltIn = this.isoPrologBuiltIns.get(this.isoPrologBuiltInKey(goal));
        if (isoPrologBuiltIn == null) {
            return Optional.empty();
        }
        return Optional.of(isoPrologBuiltIn.solve(new IsoPrologBuiltInCall(goal, s, queryTail, clauses)));
    }

    private void registerIsoPrologBuiltIns() {
        this.registerTermUnificationAndComparisonBuiltIns();
        this.registerTypeTestingBuiltIns();
        this.registerTermCreationAndDecompositionBuiltIns();
        this.registerArithmeticBuiltIns();
        this.registerControlBuiltIns();
        this.registerStreamAndFileIoBuiltIns();
        this.registerCharacterAndTermIoBuiltIns();
        this.registerKnowledgeBaseBuiltIns();
        this.registerAllSolutionsBuiltIns();
        this.registerAtomAndCharacterBuiltIns();
        this.registerListingAndMetadataBuiltIns();
        this.registerEnvironmentAndFlagBuiltIns();
    }

    private void registerDefaultFlags() {
        this.prologFlags.put("bounded", "false");
        this.prologFlags.put("integer_rounding_function", "toward_zero");
        this.prologFlags.put("max_arity", "unbounded");
        this.prologFlags.put("unknown", "fail");
        this.prologFlags.put("double_quotes", "chars");
    }

    private void registerDefaultOperators() {
        this.registerOperator(1200, "xfx", ":-");
        this.registerOperator(1200, "fx", ":-");
        this.registerOperator(1200, "fx", "?-");
        this.registerOperator(1100, "xfy", ";");
        this.registerOperator(1050, "xfy", "->");
        this.registerOperator(1000, "xfy", ",");
        this.registerOperator(700, "xfx", "=");
        this.registerOperator(700, "xfx", "\\=");
        this.registerOperator(700, "xfx", "==");
        this.registerOperator(700, "xfx", "\\==");
        this.registerOperator(700, "xfx", "@<");
        this.registerOperator(700, "xfx", "@<=");
        this.registerOperator(700, "xfx", "@>");
        this.registerOperator(700, "xfx", "@>=");
        this.registerOperator(700, "xfx", "=..");
        this.registerOperator(700, "xfx", "is");
        this.registerOperator(700, "xfx", "=:=");
        this.registerOperator(700, "xfx", "=\\=");
        this.registerOperator(700, "xfx", "<");
        this.registerOperator(700, "xfx", "=<");
        this.registerOperator(700, "xfx", ">");
        this.registerOperator(700, "xfx", ">=");
        this.registerOperator(500, "yfx", "+");
        this.registerOperator(500, "yfx", "-");
        this.registerOperator(400, "yfx", "*");
        this.registerOperator(400, "yfx", "/");
        this.registerOperator(400, "yfx", "//");
        this.registerOperator(400, "yfx", "mod");
        this.registerOperator(200, "fy", "\\+");
    }

    private void registerTermUnificationAndComparisonBuiltIns() {
        this.registerIsoPrologBuiltIn("=", 2, this::isoPrologBuiltInComparison);
        this.registerIsoPrologBuiltIn("\\=", 2, this::isoPrologBuiltInComparison);
        this.registerIsoPrologBuiltIn("==", 2, this::isoPrologBuiltInTermIdentity);
        this.registerIsoPrologBuiltIn("\\==", 2, this::isoPrologBuiltInTermNonIdentity);
        List.of("@<", "@<=", "@>", "@>=")
                .forEach(name -> this.registerIsoPrologBuiltIn(name, 2, this::isoPrologBuiltInTermOrder));
        this.registerIsoPrologBuiltIn("unify_with_occurs_check", 2, this::isoPrologBuiltInUnifyWithOccursCheck);
    }

    private void registerTypeTestingBuiltIns() {
        List.of("var", "nonvar", "atom", "integer", "float", "number", "atomic", "compound", "callable")
                .forEach(name -> this.registerIsoPrologBuiltIn(name, 1, this::isoPrologBuiltInTypeTest));
    }

    private void registerTermCreationAndDecompositionBuiltIns() {
        this.registerIsoPrologBuiltIn("functor", 3, this::isoPrologBuiltInFunctor);
        this.registerIsoPrologBuiltIn("arg", 3, this::isoPrologBuiltInArg);
        this.registerIsoPrologBuiltIn("=..", 2, this::isoPrologBuiltInUniv);
        this.registerIsoPrologBuiltIn("copy_term", 2, this::isoPrologBuiltInCopyTerm);
    }

    private void registerArithmeticBuiltIns() {
        this.registerIsoPrologBuiltIn("is", 2, this::isoPrologBuiltInIs);
        List.of("#=", ">", "#>", ">=", "#>=", "<", "#<", "=<", "#=<", "=:=", "=\\=", "#\\=")
                .forEach(name -> this.registerIsoPrologBuiltIn(name, 2, this::isoPrologBuiltInComparison));
    }

    private void registerControlBuiltIns() {
        this.registerIsoPrologBuiltIn("true", 0, this::isoPrologBuiltInTrue);
        this.registerIsoPrologBuiltIn("false", 0, this::isoPrologBuiltInFalse);
        this.registerIsoPrologBuiltIn("fail", 0, this::isoPrologBuiltInFalse);
        this.registerIsoPrologBuiltIn("!", 0, this::isoPrologBuiltInCut);
        this.registerIsoPrologBuiltIn(",", 2, this::isoPrologBuiltInConjunction);
        this.registerIsoPrologBuiltIn(";", 2, this::isoPrologBuiltInDisjunction);
        this.registerIsoPrologBuiltIn("->", 2, this::isoPrologBuiltInIfThen);
        this.registerIsoPrologBuiltIn("\\+", 1, this::isoPrologBuiltInNegation);
        this.registerIsoPrologBuiltIn("once", 1, this::isoPrologBuiltInOnce);
        this.registerIsoPrologBuiltIn("repeat", 0, this::isoPrologBuiltInTrue);
        for (int arity = 1; arity <= 8; arity++) {
            this.registerIsoPrologBuiltIn("call", arity, this::isoPrologBuiltInCall);
        }
        this.registerIsoPrologBuiltIn("catch", 3, this::isoPrologBuiltInCatch);
        this.registerIsoPrologBuiltIn("throw", 1, this::isoPrologBuiltInThrow);
    }

    private void registerStreamAndFileIoBuiltIns() {
        this.registerIsoPrologBuiltIn("open", 3, this::isoPrologBuiltInOpen);
        this.registerIsoPrologBuiltIn("open", 4, this::isoPrologBuiltInOpen);
        this.registerIsoPrologBuiltIn("close", 1, this::isoPrologBuiltInClose);
        this.registerIsoPrologBuiltIn("close", 2, this::isoPrologBuiltInClose);
        this.registerIsoPrologBuiltIn("current_input", 1, call -> this.unifyAndContinue(call, call.goal.terms.get(0), this.atom("user_input")));
        this.registerIsoPrologBuiltIn("current_output", 1, call -> this.unifyAndContinue(call, call.goal.terms.get(0), this.atom("user_output")));
        this.registerIsoPrologBuiltIn("set_input", 1, this::isoPrologBuiltInTrue);
        this.registerIsoPrologBuiltIn("set_output", 1, this::isoPrologBuiltInTrue);
        this.registerIsoPrologBuiltIn("flush_output", 0, this::isoPrologBuiltInFlushOutput);
        this.registerIsoPrologBuiltIn("flush_output", 1, this::isoPrologBuiltInFlushOutput);
    }

    private void registerCharacterAndTermIoBuiltIns() {
        this.registerIsoPrologBuiltIn("read", 1, this::isoPrologBuiltInRead);
        this.registerIsoPrologBuiltIn("read", 2, this::isoPrologBuiltInRead);
        this.registerIsoPrologBuiltIn("read_term", 2, this::isoPrologBuiltInRead);
        this.registerIsoPrologBuiltIn("read_term", 3, this::isoPrologBuiltInRead);
        this.registerIsoPrologBuiltIn("write", 1, this::isoPrologBuiltInWrite);
        this.registerIsoPrologBuiltIn("write", 2, this::isoPrologBuiltInWrite);
        this.registerIsoPrologBuiltIn("writeq", 1, this::isoPrologBuiltInWrite);
        this.registerIsoPrologBuiltIn("writeq", 2, this::isoPrologBuiltInWrite);
        this.registerIsoPrologBuiltIn("write_canonical", 1, this::isoPrologBuiltInWrite);
        this.registerIsoPrologBuiltIn("write_canonical", 2, this::isoPrologBuiltInWrite);
        this.registerIsoPrologBuiltIn("get_char", 1, call -> this.unifyAndContinue(call, call.goal.terms.get(0), this.atom("end_of_file")));
        this.registerIsoPrologBuiltIn("get_char", 2, call -> this.unifyAndContinue(call, call.goal.terms.get(1), this.atom("end_of_file")));
        this.registerIsoPrologBuiltIn("get_code", 1, call -> this.unifyAndContinue(call, call.goal.terms.get(0), this.number(-1)));
        this.registerIsoPrologBuiltIn("get_code", 2, call -> this.unifyAndContinue(call, call.goal.terms.get(1), this.number(-1)));
        this.registerIsoPrologBuiltIn("peek_char", 1, call -> this.unifyAndContinue(call, call.goal.terms.get(0), this.atom("end_of_file")));
        this.registerIsoPrologBuiltIn("peek_char", 2, call -> this.unifyAndContinue(call, call.goal.terms.get(1), this.atom("end_of_file")));
        this.registerIsoPrologBuiltIn("peek_code", 1, call -> this.unifyAndContinue(call, call.goal.terms.get(0), this.number(-1)));
        this.registerIsoPrologBuiltIn("peek_code", 2, call -> this.unifyAndContinue(call, call.goal.terms.get(1), this.number(-1)));
        this.registerIsoPrologBuiltIn("put_char", 1, this::isoPrologBuiltInPutChar);
        this.registerIsoPrologBuiltIn("put_char", 2, this::isoPrologBuiltInPutChar);
        this.registerIsoPrologBuiltIn("put_code", 1, this::isoPrologBuiltInPutCode);
        this.registerIsoPrologBuiltIn("put_code", 2, this::isoPrologBuiltInPutCode);
        this.registerIsoPrologBuiltIn("nl", 0, this::isoPrologBuiltInNl);
        this.registerIsoPrologBuiltIn("nl", 1, this::isoPrologBuiltInNl);
    }

    private void registerKnowledgeBaseBuiltIns() {
        this.registerIsoPrologBuiltIn("dynamic", 1, this::isoPrologBuiltInTrue);
        this.registerIsoPrologBuiltIn("asserta", 1, call -> this.isoPrologBuiltInAssert(call, true));
        this.registerIsoPrologBuiltIn("assertz", 1, call -> this.isoPrologBuiltInAssert(call, false));
        this.registerIsoPrologBuiltIn("retract", 1, this::isoPrologBuiltInRetract);
        this.registerIsoPrologBuiltIn("retractall", 1, this::isoPrologBuiltInRetractAll);
        this.registerIsoPrologBuiltIn("clause", 2, this::isoPrologBuiltInClause);
        this.registerIsoPrologBuiltIn("abolish", 1, this::isoPrologBuiltInAbolish);
    }

    private void registerAllSolutionsBuiltIns() {
        this.registerIsoPrologBuiltIn("findall", 3, this::isoPrologBuiltInFindAll);
        this.registerIsoPrologBuiltIn("bagof", 3, this::isoPrologBuiltInFindAll);
        this.registerIsoPrologBuiltIn("setof", 3, this::isoPrologBuiltInSetOf);
    }

    private void registerAtomAndCharacterBuiltIns() {
        this.registerIsoPrologBuiltIn("atom_length", 2, this::isoPrologBuiltInAtomLength);
        this.registerIsoPrologBuiltIn("atom_chars", 2, this::isoPrologBuiltInAtomChars);
        this.registerIsoPrologBuiltIn("atom_codes", 2, this::isoPrologBuiltInAtomCodes);
        this.registerIsoPrologBuiltIn("char_code", 2, this::isoPrologBuiltInCharCode);
        this.registerIsoPrologBuiltIn("number_chars", 2, this::isoPrologBuiltInNumberChars);
        this.registerIsoPrologBuiltIn("number_codes", 2, this::isoPrologBuiltInNumberCodes);
        this.registerIsoPrologBuiltIn("sub_atom", 5, this::isoPrologBuiltInSubAtom);
    }

    private void registerListingAndMetadataBuiltIns() {
        this.registerIsoPrologBuiltIn("current_predicate", 1, this::isoPrologBuiltInCurrentPredicate);
        this.registerIsoPrologBuiltIn("current_char_conversion", 2, this::isoPrologBuiltInCurrentCharConversion);
    }

    private void registerEnvironmentAndFlagBuiltIns() {
        this.registerIsoPrologBuiltIn("current_prolog_flag", 2, this::isoPrologBuiltInCurrentPrologFlag);
        this.registerIsoPrologBuiltIn("set_prolog_flag", 2, this::isoPrologBuiltInSetPrologFlag);
        this.registerIsoPrologBuiltIn("current_op", 3, this::isoPrologBuiltInCurrentOp);
        this.registerIsoPrologBuiltIn("op", 3, this::isoPrologBuiltInOp);
        this.registerIsoPrologBuiltIn("halt", 0, this::isoPrologBuiltInHalt);
        this.registerIsoPrologBuiltIn("halt", 1, this::isoPrologBuiltInHalt);
        this.registerIsoPrologBuiltIn("consult", 1, this::isoPrologBuiltInConsult);
    }

    private void registerIsoPrologBuiltIn(String name, int arity, IsoPrologBuiltInPredicate isoPrologBuiltIn) {
        this.isoPrologBuiltIns.put(this.isoPrologBuiltInKey(name, arity), isoPrologBuiltIn);
    }

    private String isoPrologBuiltInKey(Constr goal) {
        return this.isoPrologBuiltInKey(goal.atom.toValueString(), goal.terms.size());
    }

    private String isoPrologBuiltInKey(String name, int arity) {
        return name + "/" + arity;
    }

    private List<Subst> isoPrologBuiltInTrue(IsoPrologBuiltInCall call) throws IOException {
        return call.succeed();
    }

    private List<Subst> isoPrologBuiltInFalse(IsoPrologBuiltInCall call) {
        return call.fail();
    }

    private List<Subst> isoPrologBuiltInCut(IsoPrologBuiltInCall call) throws IOException {
        // Full cut needs choice-point tracking; for now it succeeds as a deterministic goal.
        return call.succeed();
    }

    private List<Subst> isoPrologBuiltInConjunction(IsoPrologBuiltInCall call) throws IOException {
        return call.continueWith(call.subst, new TermsList(call.goal.terms.get(0), new TermsList(call.goal.terms.get(1))).concat(call.queryTail), call.clauses);
    }

    private List<Subst> isoPrologBuiltInDisjunction(IsoPrologBuiltInCall call) throws IOException {
        var left = call.continueWith(call.subst, new TermsList(call.goal.terms.get(0)).concat(call.queryTail), call.clauses);
        var right = call.continueWith(call.subst, new TermsList(call.goal.terms.get(1)).concat(call.queryTail), call.clauses);
        var result = new ArrayList<Subst>(left);
        result.addAll(right);
        return result;
    }

    private List<Subst> isoPrologBuiltInIfThen(IsoPrologBuiltInCall call) throws IOException {
        var condition = this.solveTerm(call.goal.terms.get(0), call.subst, call.clauses);
        if (condition.isEmpty()) {
            return call.fail();
        }
        return call.continueWith(condition.get(0), new TermsList(call.goal.terms.get(1)).concat(call.queryTail), call.clauses);
    }

    private List<Subst> isoPrologBuiltInNegation(IsoPrologBuiltInCall call) throws IOException {
        return this.solveTerm(call.goal.terms.get(0), call.subst, call.clauses).isEmpty() ? call.succeed() : call.fail();
    }

    private List<Subst> isoPrologBuiltInOnce(IsoPrologBuiltInCall call) throws IOException {
        var solutions = this.solveTerm(call.goal.terms.get(0), call.subst, call.clauses);
        if (solutions.isEmpty()) {
            return call.fail();
        }
        return call.continueWith(solutions.get(0));
    }

    private List<Subst> isoPrologBuiltInCall(IsoPrologBuiltInCall call) throws IOException {
        var callable = call.goal.terms.get(0).map(call.subst);
        var callableConstr = callable.asTerm().asConstr();
        if (callableConstr.isEmpty()) {
            return call.fail();
        }

        var goal = callableConstr.get();
        if (call.goal.terms.size() > 1) {
            var args = new TermsList(goal.terms);
            args.addAll(call.goal.terms.subList(1, call.goal.terms.size()));
            goal = new Constr(goal.atom, args);
        }
        return call.continueWith(call.subst, new TermsList(goal).concat(call.queryTail), call.clauses);
    }

    private List<Subst> isoPrologBuiltInCatch(IsoPrologBuiltInCall call) throws IOException {
        try {
            return call.continueWith(call.subst, new TermsList(call.goal.terms.get(0)).concat(call.queryTail), call.clauses);
        } catch (ThrownPrologException e) {
            var catcher = call.goal.terms.get(1).unify(e.term, call.subst);
            if (catcher.isEmpty()) {
                throw e;
            }
            return call.continueWith(catcher.get(), new TermsList(call.goal.terms.get(2)).concat(call.queryTail), call.clauses);
        }
    }

    private List<Subst> isoPrologBuiltInThrow(IsoPrologBuiltInCall call) {
        throw new ThrownPrologException(call.goal.terms.get(0).map(call.subst).asTerm());
    }

    private List<Subst> isoPrologBuiltInTermIdentity(IsoPrologBuiltInCall call) throws IOException {
        return this.canonicalTerm(call.goal.terms.get(0).map(call.subst)).equals(this.canonicalTerm(call.goal.terms.get(1).map(call.subst)))
                ? call.succeed()
                : call.fail();
    }

    private List<Subst> isoPrologBuiltInTermNonIdentity(IsoPrologBuiltInCall call) throws IOException {
        return this.canonicalTerm(call.goal.terms.get(0).map(call.subst)).equals(this.canonicalTerm(call.goal.terms.get(1).map(call.subst)))
                ? call.fail()
                : call.succeed();
    }

    private List<Subst> isoPrologBuiltInTermOrder(IsoPrologBuiltInCall call) throws IOException {
        var comparison = this.canonicalTerm(call.goal.terms.get(0).map(call.subst))
                .compareTo(this.canonicalTerm(call.goal.terms.get(1).map(call.subst)));
        var operator = call.goal.atom.toValueString();
        var matches = switch (operator) {
            case "@<" -> comparison < 0;
            case "@<=" -> comparison <= 0;
            case "@>" -> comparison > 0;
            case "@>=" -> comparison >= 0;
            default -> false;
        };
        return matches ? call.succeed() : call.fail();
    }

    private List<Subst> isoPrologBuiltInUnifyWithOccursCheck(IsoPrologBuiltInCall call) throws IOException {
        var unified = call.goal.terms.get(0).unify(call.goal.terms.get(1), call.subst);
        if (unified.isEmpty()) {
            return call.fail();
        }
        return call.continueWith(unified.get());
    }

    private List<Subst> isoPrologBuiltInTypeTest(IsoPrologBuiltInCall call) throws IOException {
        var term = call.goal.terms.get(0).map(call.subst);
        var type = call.goal.atom.toValueString();
        var matches = switch (type) {
            case "var" -> term.asVar().isPresent();
            case "nonvar" -> term.asVar().isEmpty();
            case "atom" -> this.isAtom(term);
            case "integer" -> this.isInteger(term);
            case "float" -> this.isFloat(term);
            case "number" -> this.isNumber(term);
            case "atomic" -> this.isAtom(term) || this.isNumber(term);
            case "compound" -> term.asTerm().asConstr().filter(constr -> !constr.terms.isEmpty()).isPresent();
            case "callable" -> this.isAtom(term) || term.asTerm().asConstr().filter(constr -> !constr.terms.isEmpty()).isPresent();
            default -> false;
        };
        return matches ? call.succeed() : call.fail();
    }

    private List<Subst> isoPrologBuiltInFunctor(IsoPrologBuiltInCall call) throws IOException {
        var term = call.goal.terms.get(0).map(call.subst);
        var name = call.goal.terms.get(1).map(call.subst);
        var arity = call.goal.terms.get(2).map(call.subst);
        var termConstr = term.asTerm().asConstr();

        if (termConstr.isPresent() && term.asVar().isEmpty()) {
            var constr = termConstr.get();
            var subst = call.goal.terms.get(1).unify(this.atom(constr.atom.toValueString()), call.subst)
                    .flatMap(s -> call.goal.terms.get(2).unify(this.number(constr.terms.size()), s));
            return subst.map(call::continueWithUnchecked).orElseGet(call::fail);
        }

        var nameConstr = name.asTerm().asConstr();
        var arityNumber = this.integerValue(arity);
        if (nameConstr.isEmpty() || arityNumber.isEmpty() || arityNumber.get() < 0) {
            return call.fail();
        }

        var created = arityNumber.get() == 0
                ? this.atom(nameConstr.get().atom.toValueString())
                : new Constr(nameConstr.get().atom, this.freshVariables(arityNumber.get(), "_F"));
        var subst = call.goal.terms.get(0).unify(created, call.subst);
        return subst.map(call::continueWithUnchecked).orElseGet(call::fail);
    }

    private List<Subst> isoPrologBuiltInArg(IsoPrologBuiltInCall call) throws IOException {
        var index = this.integerValue(call.goal.terms.get(0).map(call.subst));
        var term = call.goal.terms.get(1).map(call.subst).asTerm().asConstr();
        if (index.isEmpty() || term.isEmpty() || index.get() < 1 || index.get() > term.get().terms.size()) {
            return call.fail();
        }
        return this.unifyAndContinue(call, call.goal.terms.get(2), term.get().terms.get(index.get() - 1));
    }

    private List<Subst> isoPrologBuiltInUniv(IsoPrologBuiltInCall call) throws IOException {
        var term = call.goal.terms.get(0).map(call.subst);
        var list = call.goal.terms.get(1).map(call.subst);
        if (term.asVar().isEmpty()) {
            var constr = term.asTerm().asConstr();
            if (constr.isEmpty()) {
                return call.fail();
            }
            var elements = new ArrayList<Term>();
            elements.add(this.atom(constr.get().atom.toValueString()));
            elements.addAll(constr.get().terms);
            return this.unifyAndContinue(call, call.goal.terms.get(1), this.list(elements));
        }

        var elements = this.listElements(list);
        if (elements.isEmpty() || elements.get().isEmpty()) {
            return call.fail();
        }
        var name = elements.get().get(0).asTerm().asConstr();
        if (name.isEmpty()) {
            return call.fail();
        }
        var args = elements.get().subList(1, elements.get().size());
        var created = args.isEmpty() ? this.atom(name.get().atom.toValueString()) : new Constr(name.get().atom, new TermsList(args));
        return this.unifyAndContinue(call, call.goal.terms.get(0), created);
    }

    private List<Subst> isoPrologBuiltInCopyTerm(IsoPrologBuiltInCall call) throws IOException {
        var source = call.goal.terms.get(0).map(call.subst);
        var copied = source.map(source.freevars().asSubs());
        return this.unifyAndContinue(call, call.goal.terms.get(1), copied);
    }

    private List<Subst> isoPrologBuiltInAssert(IsoPrologBuiltInCall call, boolean atStart) throws IOException {
        var clause = this.assertedClause(call.goal.terms.get(0).map(call.subst));
        if (clause.isEmpty()) {
            return call.fail();
        }

        this.top().memory.assertClause(clause.get(), atStart);
        return call.continueWith(call.subst, this.top().memory.clauses().collect(Collectors.toList()));
    }

    private List<Subst> isoPrologBuiltInRetract(IsoPrologBuiltInCall call) {
        return call.fail();
    }

    private List<Subst> isoPrologBuiltInRetractAll(IsoPrologBuiltInCall call) throws IOException {
        return call.succeed();
    }

    private List<Subst> isoPrologBuiltInClause(IsoPrologBuiltInCall call) throws IOException {
        var results = new ArrayList<Subst>();
        for (var clause: this.top().memory.clauses().collect(Collectors.toList())) {
            var head = clause.lhs();
            var body = (Terms)clause.rhs();
            var unified = call.goal.terms.get(0).unify(head, call.subst)
                    .flatMap(s -> call.goal.terms.get(1).unify(body.isEmpty() ? this.atom("true") : this.bodyTerm(body), s));
            if (unified.isPresent()) {
                results.addAll(call.continueWith(unified.get()));
            }
        }
        return results;
    }

    private List<Subst> isoPrologBuiltInAbolish(IsoPrologBuiltInCall call) throws IOException {
        return call.succeed();
    }

    private List<Subst> isoPrologBuiltInConsult(IsoPrologBuiltInCall call) throws IOException {
        var fileName = this.consultFileName(call.goal.terms.get(0).map(call.subst));
        if (fileName.isEmpty()) {
            return call.fail();
        }

        try (var reader = new FileReader(fileName.get())) {
            this.consult(reader);
        }
        return call.continueWith(call.subst, this.top().memory.clauses().collect(Collectors.toList()));
    }

    private List<Subst> isoPrologBuiltInOpen(IsoPrologBuiltInCall call) throws IOException {
        var source = this.atomText(call.goal.terms.get(0).map(call.subst));
        var mode = this.atomText(call.goal.terms.get(1).map(call.subst));
        if (source.isEmpty() || mode.isEmpty()) {
            return call.fail();
        }
        return this.unifyAndContinue(call, call.goal.terms.get(2), this.atom("stream_" + Math.abs((source.get() + mode.get()).hashCode())));
    }

    private List<Subst> isoPrologBuiltInClose(IsoPrologBuiltInCall call) throws IOException {
        return call.succeed();
    }

    private List<Subst> isoPrologBuiltInFlushOutput(IsoPrologBuiltInCall call) throws IOException {
        System.out.flush();
        return call.succeed();
    }

    private List<Subst> isoPrologBuiltInRead(IsoPrologBuiltInCall call) throws IOException {
        var targetIndex = call.goal.terms.size() == 1 ? 0 : call.goal.terms.size() - 2;
        return this.unifyAndContinue(call, call.goal.terms.get(targetIndex), this.atom("end_of_file"));
    }

    private List<Subst> isoPrologBuiltInWrite(IsoPrologBuiltInCall call) throws IOException {
        var valueIndex = call.goal.terms.size() == 1 ? 0 : 1;
        System.out.print(this.termToOutputString(call.goal.terms.get(valueIndex).map(call.subst)));
        return call.succeed();
    }

    private List<Subst> isoPrologBuiltInNl(IsoPrologBuiltInCall call) throws IOException {
        System.out.println();
        return call.succeed();
    }

    private List<Subst> isoPrologBuiltInPutChar(IsoPrologBuiltInCall call) throws IOException {
        var valueIndex = call.goal.terms.size() == 1 ? 0 : 1;
        var text = this.atomText(call.goal.terms.get(valueIndex).map(call.subst));
        if (text.isEmpty() || text.get().length() != 1) {
            return call.fail();
        }
        System.out.print(text.get());
        return call.succeed();
    }

    private List<Subst> isoPrologBuiltInPutCode(IsoPrologBuiltInCall call) throws IOException {
        var valueIndex = call.goal.terms.size() == 1 ? 0 : 1;
        var code = this.integerValue(call.goal.terms.get(valueIndex).map(call.subst));
        if (code.isEmpty()) {
            return call.fail();
        }
        System.out.print((char)code.get().intValue());
        return call.succeed();
    }

    private List<Subst> isoPrologBuiltInIs(IsoPrologBuiltInCall call) throws IOException {
        var value = this.evaluateNumber(call.goal.terms.get(1), call.subst);
        if (value.isEmpty()) {
            return call.fail();
        }

        var numberTerm = new TokenValue(Token.NUMBER, value.get());
        var newS = call.goal.terms.get(0).unify(numberTerm, call.subst);
        if (newS.isEmpty()) {
            return call.fail();
        }
        return call.continueWith(newS.get());
    }

    private List<Subst> isoPrologBuiltInComparison(IsoPrologBuiltInCall call) throws IOException {
        var comparison = this.evaluateComparison(call.goal, call.subst);
        if (comparison.isPresent()) {
            return call.continueWith(comparison.get());
        }
        if (!call.queryTail.isEmpty() && this.hasUnboundVariables(call.goal, call.subst)) {
            return call.continueWith(call.subst, call.queryTail.concat(call.goal), call.clauses);
        }
        return call.fail();
    }

    private List<Subst> isoPrologBuiltInFindAll(IsoPrologBuiltInCall call) throws IOException {
        var template = call.goal.terms.get(0);
        var goal = call.goal.terms.get(1);
        var solutions = this.solveTerm(goal, call.subst, call.clauses);
        var values = solutions.stream().map(template::map).toList();
        return this.unifyAndContinue(call, call.goal.terms.get(2), this.list(values));
    }

    private List<Subst> isoPrologBuiltInSetOf(IsoPrologBuiltInCall call) throws IOException {
        var template = call.goal.terms.get(0);
        var goal = call.goal.terms.get(1);
        var solutions = this.solveTerm(goal, call.subst, call.clauses);
        var seen = new HashSet<String>();
        var values = solutions.stream()
                .map(template::map)
                .sorted(Comparator.comparing(this::canonicalTerm))
                .filter(term -> seen.add(this.canonicalTerm(term)))
                .toList();
        return this.unifyAndContinue(call, call.goal.terms.get(2), this.list(values));
    }

    private List<Subst> isoPrologBuiltInAtomLength(IsoPrologBuiltInCall call) throws IOException {
        var atom = this.atomText(call.goal.terms.get(0).map(call.subst));
        if (atom.isEmpty()) {
            return call.fail();
        }
        return this.unifyAndContinue(call, call.goal.terms.get(1), this.number(atom.get().length()));
    }

    private List<Subst> isoPrologBuiltInAtomChars(IsoPrologBuiltInCall call) throws IOException {
        return this.textListBuiltIn(call, false, false);
    }

    private List<Subst> isoPrologBuiltInAtomCodes(IsoPrologBuiltInCall call) throws IOException {
        return this.textListBuiltIn(call, true, false);
    }

    private List<Subst> isoPrologBuiltInCharCode(IsoPrologBuiltInCall call) throws IOException {
        var character = this.atomText(call.goal.terms.get(0).map(call.subst));
        var code = this.integerValue(call.goal.terms.get(1).map(call.subst));
        if (character.isPresent() && character.get().length() == 1) {
            return this.unifyAndContinue(call, call.goal.terms.get(1), this.number((int)character.get().charAt(0)));
        }
        if (code.isPresent()) {
            return this.unifyAndContinue(call, call.goal.terms.get(0), this.atom(String.valueOf((char)code.get().intValue())));
        }
        return call.fail();
    }

    private List<Subst> isoPrologBuiltInNumberChars(IsoPrologBuiltInCall call) throws IOException {
        return this.textListBuiltIn(call, false, true);
    }

    private List<Subst> isoPrologBuiltInNumberCodes(IsoPrologBuiltInCall call) throws IOException {
        return this.textListBuiltIn(call, true, true);
    }

    private List<Subst> isoPrologBuiltInSubAtom(IsoPrologBuiltInCall call) throws IOException {
        var atom = this.atomText(call.goal.terms.get(0).map(call.subst));
        var before = this.integerValue(call.goal.terms.get(1).map(call.subst));
        var length = this.integerValue(call.goal.terms.get(2).map(call.subst));
        if (atom.isEmpty() || before.isEmpty() || length.isEmpty()) {
            return call.fail();
        }
        var text = atom.get();
        var start = before.get();
        var end = start + length.get();
        if (start < 0 || end > text.length()) {
            return call.fail();
        }
        var after = text.length() - end;
        var subst = call.goal.terms.get(3).unify(this.number(after), call.subst)
                .flatMap(s -> call.goal.terms.get(4).unify(this.atom(text.substring(start, end)), s));
        return subst.map(call::continueWithUnchecked).orElseGet(call::fail);
    }

    private List<Subst> isoPrologBuiltInCurrentPrologFlag(IsoPrologBuiltInCall call) throws IOException {
        var results = new ArrayList<Subst>();
        for (var entry: new ArrayList<>(this.prologFlags.entrySet())) {
            var subst = call.goal.terms.get(0).unify(this.atom(entry.getKey()), call.subst)
                    .flatMap(s -> call.goal.terms.get(1).unify(this.atom(entry.getValue()), s));
            if (subst.isPresent()) {
                results.addAll(call.continueWith(subst.get()));
            }
        }
        return results;
    }

    private List<Subst> isoPrologBuiltInCurrentPredicate(IsoPrologBuiltInCall call) throws IOException {
        var results = new ArrayList<Subst>();
        for (var predicateIndicator: this.currentPredicateIndicators()) {
            var subst = call.goal.terms.get(0).unify(predicateIndicator, call.subst);
            if (subst.isPresent()) {
                results.addAll(call.continueWith(subst.get()));
            }
        }
        return results;
    }

    private List<Subst> isoPrologBuiltInCurrentCharConversion(IsoPrologBuiltInCall call) throws IOException {
        var results = new ArrayList<Subst>();
        for (var entry: this.charConversions.entrySet()) {
            var subst = call.goal.terms.get(0).unify(this.atom(entry.getKey()), call.subst)
                    .flatMap(s -> call.goal.terms.get(1).unify(this.atom(entry.getValue()), s));
            if (subst.isPresent()) {
                results.addAll(call.continueWith(subst.get()));
            }
        }
        return results;
    }

    private List<Subst> isoPrologBuiltInSetPrologFlag(IsoPrologBuiltInCall call) throws IOException {
        var key = this.atomText(call.goal.terms.get(0).map(call.subst));
        if (key.isEmpty()) {
            return call.fail();
        }
        this.prologFlags.put(key.get(), this.termToOutputString(call.goal.terms.get(1).map(call.subst)));
        return call.succeed();
    }

    private List<Subst> isoPrologBuiltInCurrentOp(IsoPrologBuiltInCall call) throws IOException {
        var results = new ArrayList<Subst>();
        for (var op: new ArrayList<>(this.operatorDeclarations)) {
            var subst = call.goal.terms.get(0).unify(op.terms.get(0), call.subst)
                    .flatMap(s -> call.goal.terms.get(1).unify(op.terms.get(1), s))
                    .flatMap(s -> call.goal.terms.get(2).unify(op.terms.get(2), s));
            if (subst.isPresent()) {
                results.addAll(call.continueWith(subst.get()));
            }
        }
        return results;
    }

    private List<Subst> isoPrologBuiltInOp(IsoPrologBuiltInCall call) throws IOException {
        this.operatorDeclarations.add(new Constr(this.atom("op"), List.of(
                call.goal.terms.get(0).map(call.subst),
                call.goal.terms.get(1).map(call.subst),
                call.goal.terms.get(2).map(call.subst))));
        return call.succeed();
    }

    private List<Subst> isoPrologBuiltInHalt(IsoPrologBuiltInCall call) throws IOException {
        throw new IOException("halt");
    }

    private String termToOutputString(Term term) {
        return term.asTerm().append(new StringBuilder()).toString();
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

    private void registerOperator(int priority, String specifier, String operator) {
        this.operatorDeclarations.add(new Constr(this.atom("op"), List.of(
                this.number(priority),
                this.atom(specifier),
                this.atom(operator))));
    }

    private List<Term> currentPredicateIndicators() {
        var indicators = new LinkedHashMap<String, Term>();
        this.isoPrologBuiltIns.keySet().forEach(key -> indicators.put(key, this.predicateIndicator(key)));
        this.top().memory.facts().forEach(entry -> indicators.put(entry.getKey(), this.predicateIndicator(entry.getKey())));
        this.top().memory.rules().forEach(entry -> indicators.put(entry.getKey(), this.predicateIndicator(entry.getKey())));
        this.top().memory.clauses().forEach(clause -> {
            var head = clause.lhs().asTerm().asConstr();
            head.ifPresent(constr -> indicators.put(
                    this.isoPrologBuiltInKey(constr.atom.toValueString(), constr.terms.size()),
                    this.predicateIndicator(constr.atom.toValueString(), constr.terms.size())));
        });
        return new ArrayList<>(indicators.values());
    }

    private Term predicateIndicator(String key) {
        var slash = key.lastIndexOf('/');
        if (slash < 0) {
            return this.atom(key);
        }
        return this.predicateIndicator(key.substring(0, slash), Integer.parseInt(key.substring(slash + 1)));
    }

    private Term predicateIndicator(String name, int arity) {
        return new Constr(new TokenValue(Token.ARITHMETIC_OPERATOR, "/"), List.of(this.atom(name), this.number(arity)));
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
        var constr = mappedTerm.asTerm().asConstr();
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

    private List<Subst> solveTerm(Term goal, Subst subst, List<Terms> clauses) throws IOException {
        var mapped = goal.map(subst);
        if (mapped.asTerm().asConstr().isEmpty()) {
            return Collections.emptyList();
        }
        return this.solve1(new TermsList(mapped), subst, clauses);
    }

    private List<Subst> unifyAndContinue(IsoPrologBuiltInCall call, Term left, Term right) throws IOException {
        var subst = left.unify(right, call.subst);
        if (subst.isEmpty()) {
            return call.fail();
        }
        return call.continueWith(subst.get());
    }

    private TokenValue atom(String value) {
        return new TokenValue(Token.ATOM, value);
    }

    private TokenValue number(Number value) {
        return new TokenValue(Token.NUMBER, value);
    }

    private List<Term> freshVariables(int arity, String prefix) {
        var variables = new ArrayList<Term>();
        for (int i = 0; i < arity; i++) {
            variables.add(new Var(new TokenValue(Token.VARIABLE, prefix + i)));
        }
        return variables;
    }

    private Term list(List<Term> elements) {
        Term tail = TokenValue.NIL;
        for (int i = elements.size() - 1; i >= 0; i--) {
            tail = new ListConstr(this.atom("."), List.of(elements.get(i), tail));
        }
        return tail;
    }

    private Optional<List<Term>> listElements(Term term) {
        var elements = new ArrayList<Term>();
        var current = term;
        while (true) {
            current = current.asTerm();
            if (this.isNil(current)) {
                return Optional.of(elements);
            }
            var constr = current.asConstr();
            if (constr.isEmpty() || !constr.get().atom.toValueString().equals(".") || constr.get().terms.size() != 2) {
                return Optional.empty();
            }
            elements.add(constr.get().terms.get(0));
            current = constr.get().terms.get(1);
        }
    }

    private boolean isNil(Term term) {
        if (term.equals(TokenValue.NIL)) {
            return true;
        }
        var constr = term.asTerm().asConstr();
        return constr.isPresent() && constr.get().atom.is(Token.nil) && constr.get().terms.isEmpty();
    }

    private Term bodyTerm(Terms body) {
        if (body.size() == 1) {
            return body.lhs();
        }
        return new Constr(this.atom(","), body);
    }

    private boolean isAtom(Term term) {
        var constr = term.asTerm().asConstr();
        return constr.isPresent() && constr.get().terms.isEmpty() && constr.get().atom.is(Token.ATOM, Token.QUOTED_ATOM, Token.nil, Token.CUT);
    }

    private boolean isNumber(Term term) {
        var constr = term.asTerm().asConstr();
        return constr.isPresent() && constr.get().terms.isEmpty() && constr.get().atom.is(Token.NUMBER);
    }

    private boolean isInteger(Term term) {
        var constr = term.asTerm().asConstr();
        return constr.isPresent()
                && constr.get().terms.isEmpty()
                && constr.get().atom.is(Token.NUMBER)
                && constr.get().atom.value instanceof Number number
                && number.doubleValue() == Math.rint(number.doubleValue());
    }

    private boolean isFloat(Term term) {
        var constr = term.asTerm().asConstr();
        return constr.isPresent()
                && constr.get().terms.isEmpty()
                && constr.get().atom.is(Token.NUMBER)
                && (constr.get().atom.value instanceof Float || constr.get().atom.value instanceof Double);
    }

    private Optional<Integer> integerValue(Term term) {
        var constr = term.asTerm().asConstr();
        if (constr.isEmpty() || !constr.get().terms.isEmpty() || !constr.get().atom.is(Token.NUMBER)) {
            return Optional.empty();
        }
        return Optional.of(((Number)constr.get().atom.value).intValue());
    }

    private Optional<String> atomText(Term term) {
        var constr = term.asTerm().asConstr();
        if (constr.isEmpty() || !constr.get().terms.isEmpty() || !constr.get().atom.is(Token.ATOM, Token.QUOTED_ATOM, Token.nil)) {
            return Optional.empty();
        }
        return Optional.of(constr.get().atom.toValueString());
    }

    private String canonicalTerm(Term term) {
        return term.asTerm().map(new Subst()).append(new StringBuilder()).toString();
    }

    private List<Subst> textListBuiltIn(IsoPrologBuiltInCall call, boolean codes, boolean numberText) throws IOException {
        var left = call.goal.terms.get(0).map(call.subst);
        var right = call.goal.terms.get(1).map(call.subst);
        var leftText = numberText ? this.numberText(left) : this.atomText(left);
        if (leftText.isPresent()) {
            return this.unifyAndContinue(call, call.goal.terms.get(1), this.textToList(leftText.get(), codes));
        }

        var elements = this.listElements(right);
        if (elements.isEmpty()) {
            return call.fail();
        }
        var text = this.listToText(elements.get(), codes);
        if (text.isEmpty()) {
            return call.fail();
        }
        Term converted = numberText ? this.parseNumberTerm(text.get()).orElse(null) : this.atom(text.get());
        if (converted == null) {
            return call.fail();
        }
        return this.unifyAndContinue(call, call.goal.terms.get(0), converted);
    }

    private Optional<String> numberText(Term term) {
        var constr = term.asTerm().asConstr();
        if (constr.isEmpty() || !constr.get().terms.isEmpty() || !constr.get().atom.is(Token.NUMBER)) {
            return Optional.empty();
        }
        return Optional.of(constr.get().atom.toValueString());
    }

    private Term textToList(String text, boolean codes) {
        var elements = new ArrayList<Term>();
        for (int i = 0; i < text.length(); i++) {
            elements.add(codes ? this.number((int)text.charAt(i)) : this.atom(String.valueOf(text.charAt(i))));
        }
        return this.list(elements);
    }

    private Optional<String> listToText(List<Term> elements, boolean codes) {
        var builder = new StringBuilder();
        for (var element: elements) {
            if (codes) {
                var code = this.integerValue(element);
                if (code.isEmpty()) {
                    return Optional.empty();
                }
                builder.append((char)code.get().intValue());
            } else {
                var atom = this.atomText(element);
                if (atom.isEmpty() || atom.get().length() != 1) {
                    return Optional.empty();
                }
                builder.append(atom.get());
            }
        }
        return Optional.of(builder.toString());
    }

    private Optional<Term> parseNumberTerm(String text) {
        try {
            if (text.contains(".")) {
                return Optional.of(this.number(Double.parseDouble(text)));
            }
            return Optional.of(this.number(Long.parseLong(text)));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    private interface IsoPrologBuiltInPredicate {
        List<Subst> solve(IsoPrologBuiltInCall call) throws IOException;
    }

    private class IsoPrologBuiltInCall {
        private final Constr goal;
        private final Subst subst;
        private final Terms queryTail;
        private final List<Terms> clauses;

        private IsoPrologBuiltInCall(Constr goal, Subst subst, Terms queryTail, List<Terms> clauses) {
            this.goal = goal;
            this.subst = subst;
            this.queryTail = queryTail;
            this.clauses = clauses;
        }

        private List<Subst> succeed() throws IOException {
            return this.continueWith(this.subst);
        }

        private List<Subst> fail() {
            return Collections.emptyList();
        }

        private List<Subst> continueWith(Subst subst) throws IOException {
            return this.continueWith(subst, this.clauses);
        }

        private List<Subst> continueWith(Subst subst, List<Terms> clauses) throws IOException {
            return this.continueWith(subst, this.queryTail, clauses);
        }

        private List<Subst> continueWith(Subst subst, Terms query, List<Terms> clauses) throws IOException {
            return PrologRuntime.this.solve1(query, subst, clauses);
        }

        private List<Subst> continueWithUnchecked(Subst subst) {
            try {
                return this.continueWith(subst);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private static class ThrownPrologException extends RuntimeException {
        private final Term term;

        private ThrownPrologException(Term term) {
            this.term = term;
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

    public void consultingModeOn() {
        this.inConsultingMode = true;
    }
    public void consultingModeOff() {
        this.inConsultingMode = false;
    }
}
