package prolog;

import org.junit.jupiter.api.Test;
import prolog.interpreter.PrologRuntime;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class IsoBestPracticeConformityTest extends Tester {

    private String run(String program, String query) throws IOException {
        var runtime = new PrologRuntime();
        runtime.setSolutionContinuationReader(() -> ';');
        if (program != null && !program.isBlank()) {
            runtime.consult(this.parse(program));
        }

        var output = new ByteArrayOutputStream();
        var previousOut = System.out;
        try {
            System.setOut(new PrintStream(output));
            runtime.execute(this.parse(query));
        } finally {
            System.setOut(previousOut);
        }
        return output.toString();
    }

    private String compact(String output) {
        return output.replace(" ", "").replace(System.lineSeparator(), "\n");
    }

    @Test
    public void lexer_accepts_implemented_iso_and_clpz_tokens() throws IOException {
        var tokens = new Lexer("?- =(a,b), \\=(a,b), ==(a,a), \\==(a,b), @<(a,b), @<=(a,b), @>(b,a), @>=(b,b), =..(f(a), L), #>(2,1), #=<(1,2), #\\=(1,2), \\+(fail), !, A is 1 + 2, [a|B], \"xy\", 0'a.").tokens();
        var text = tokens.toString();

        assertTrue(text.contains("?-"), text);
        assertTrue(text.contains("="), text);
        assertTrue(text.contains("\\="), text);
        assertTrue(text.contains("=="), text);
        assertTrue(text.contains("\\=="), text);
        assertTrue(text.contains("@<"), text);
        assertTrue(text.contains("@<="), text);
        assertTrue(text.contains("@>"), text);
        assertTrue(text.contains("@>="), text);
        assertTrue(text.contains("=.."), text);
        assertTrue(text.contains("#>"), text);
        assertTrue(text.contains("#=<"), text);
        assertTrue(text.contains("#\\="), text);
        assertTrue(text.contains("\\+"), text);
        assertTrue(text.contains("!"), text);
        assertTrue(text.contains("is"), text);
        assertTrue(text.contains("[120, 121]"), text);
        assertTrue(text.contains("97"), text);
    }

    @Test
    public void parser_accepts_implemented_iso_forms() throws IOException {
        var groupedRule = this.parse("assertz((grandparent(X, Y) :- parent(X, Z), parent(Z, Y))).").toString();
        var queryPrefix = this.parse("?- true, \\+(false), once(=(X, a)).").toString();
        var callableOperators = this.parse("=..(f(a,b), L), ==(a,a), @<=(a,b), #=(X, +(1, 1)).").toString();

        assertTrue(groupedRule.contains("assertz(:-(grandparent"), groupedRule);
        assertTrue(queryPrefix.contains("\\+(false)"), queryPrefix);
        assertTrue(callableOperators.contains("=.."), callableOperators);
        assertTrue(callableOperators.contains("@<="), callableOperators);
        assertTrue(callableOperators.contains("#="), callableOperators);
    }

    @Test
    public void truth_failure_and_unification_are_iso_conformant() throws IOException {
        var output = this.run("", "true, =(X, a), \\=(a, b), ==(a, a), \\==(a, b).");

        assertTrue(output.contains("X = a"), output);
    }

    @Test
    public void type_testing_predicates_identify_runtime_terms() throws IOException {
        var output = this.run("", "var(X), nonvar(a), atom(a), integer(1), number(1), atomic(a), compound(f(a)), callable(f(a)).");

        assertTrue(output.contains("true."), output);
    }

    @Test
    public void term_decomposition_predicates_inspect_terms() throws IOException {
        var output = this.run("", "functor(f(a,b), Name, Arity), arg(2, f(a,b), Arg), =..(f(a,b), List).");
        var compact = output.replace(" ", "");

        assertTrue(output.contains("Name = f"), output);
        assertTrue(output.contains("Arity = 2"), output);
        assertTrue(output.contains("Arg = b"), output);
        assertTrue(compact.contains("List=[f|[a|[b|[]]]]"), output);
    }

    @Test
    public void term_decomposition_predicates_can_construct_terms() throws IOException {
        var output = this.run("", "functor(Term, pair, 2), arg(1, Term, A), =..(Built, [pair, left, right]).");
        var compact = this.compact(output);

        assertTrue(output.contains("Term = pair("), output);
        assertTrue(output.contains("A = _F0"), output);
        assertTrue(compact.contains("Built=pair(left,right)"), output);
    }

    @Test
    public void arithmetic_and_clpz_comparisons_are_supported() throws IOException {
        var output = this.run("", "X is +(1, *(2, 3)), X #= 7, X #> 6, X #>= 7, X #< 8, X #=< 7, X #\\= 8, 7 =:= +(3, 4), 7 =\\= 8.");

        assertTrue(output.contains("X = 7"), output);
    }

    @Test
    public void recursive_rules_lists_and_delayed_clpz_goals_work_together() throws IOException {
        var program = "list_length([], 0)." +
                "list_length([_|Ls], L) :- L #= L0 + 1, L #> 0, list_length(Ls, L0).";
        var output = this.run(program, "list_length([a,b], X).");

        assertTrue(output.contains("X = 2"), output);
    }

    @Test
    public void grouped_rule_assertion_and_dynamic_ordering_are_supported() throws IOException {
        var output = this.run(
                "parent(toni,lolo). parent(lolo,yannick). source(static). kind(static).",
                "assertz((grandparent(X, Y) :- parent(X, Z), parent(Z, Y))), asserta((source(a_first) :- kind(static))), assertz((source(z_last) :- kind(static))), grandparent(toni, Who), source(Source).");

        assertTrue(output.contains("Who = yannick"), output);
        assertTrue(output.indexOf("Source = a_first") >= 0, output);
        assertTrue(output.indexOf("Source = static") > output.indexOf("Source = a_first"), output);
        assertTrue(output.indexOf("Source = z_last") > output.indexOf("Source = static"), output);
    }

    @Test
    public void all_solutions_collectors_return_expected_lists() throws IOException {
        var output = this.run("color(red). color(blue).", "findall(X, color(X), Xs), setof(X, color(X), Sorted).");
        var compact = output.replace(" ", "");

        assertTrue(compact.contains("Xs=[red|[blue|[]]]"), output);
        assertTrue(compact.contains("Sorted=[blue|[red|[]]]"), output);
    }

    @Test
    public void atom_character_and_number_conversion_predicates_are_supported() throws IOException {
        var output = this.run("", "atom_length(hello, L), atom_chars(hi, Cs), atom_codes(hi, Codes), char_code(a, Code), char_code(Char, 122), number_chars(12, Ns), number_codes(34, Nc), sub_atom(hello, 1, 3, After, Sub).");
        var compact = this.compact(output);

        assertTrue(output.contains("L = 5"), output);
        assertTrue(compact.contains("Cs=[h|[i|[]]]"), output);
        assertTrue(compact.contains("Codes=[104|[105|[]]]"), output);
        assertTrue(output.contains("Code = 97"), output);
        assertTrue(output.contains("Char = z"), output);
        assertTrue(compact.contains("Ns=[1|[2|[]]]"), output);
        assertTrue(compact.contains("Nc=[51|[52|[]]]"), output);
        assertTrue(output.contains("After = 1"), output);
        assertTrue(output.contains("Sub = ell"), output);
    }

    @Test
    public void listing_and_metadata_predicates_expose_runtime_state() throws IOException {
        var output = this.run("parent(toni,lolo).", "current_predicate(/(parent, 2)), current_op(700, xfx, '='), current_char_conversion(_, _).");

        assertTrue(output.contains("false."), output);

        output = this.run("parent(toni,lolo).", "current_predicate(/(parent, 2)), current_op(700, xfx, '=').");
        assertTrue(output.contains("true."), output);
    }

    @Test
    public void environment_stream_and_file_metadata_predicates_are_supported() throws IOException {
        var output = this.run("", "current_input(In), current_output(Out), open('tmp.pl', read, Stream), close(Stream), flush_output, set_input(In), set_output(Out), current_prolog_flag(unknown, Flag), set_prolog_flag(answer, yes), current_prolog_flag(answer, Answer).");

        assertTrue(output.contains("In = user_input"), output);
        assertTrue(output.contains("Out = user_output"), output);
        assertTrue(output.contains("Stream = stream_"), output);
        assertTrue(output.contains("Flag = fail"), output);
        assertTrue(output.contains("Answer = yes"), output);
    }

    @Test
    public void consult_loads_files_during_queries() throws IOException {
        var programFile = Files.createTempFile("myo-iso-best-practice", ".pl");
        Files.writeString(programFile, "edge(a,b). path(X,Y) :- edge(X,Y).");
        try {
            var output = this.run("", "consult('" + programFile + "'), path(a, Y).");
            assertTrue(output.contains("Y = b"), output);
        } finally {
            Files.deleteIfExists(programFile);
        }
    }

    @Test
    public void control_meta_predicates_are_supported() throws IOException {
        var output = this.run("color(red). color(blue).", "\\+(color(green)), once(color(X)), call(color, blue), catch(throw(problem), problem, true).");

        assertTrue(output.contains("X = red"), output);
    }

    @Test
    public void knowledge_base_metadata_predicates_are_supported() throws IOException {
        var output = this.run("parent(toni,lolo).", "dynamic(/(parent, 2)), clause(parent(toni,lolo), Body), \\+(retract(parent(unknown, missing))), retractall(parent(_,_)), abolish(/(parent, 2)).");

        assertTrue(output.contains("Body = true"), output);
    }

    @Test
    public void write_and_newline_emit_visible_output() throws IOException {
        var output = this.run("", "write(hello), nl, writeq(world), nl, write_canonical(f(a)), nl, put_char('!'), put_code(10).");

        assertTrue(output.contains("hello" + System.lineSeparator()), output);
        assertTrue(output.contains("world" + System.lineSeparator()), output);
        assertTrue(output.contains("f(a)" + System.lineSeparator()), output);
        assertTrue(output.contains("!" + System.lineSeparator()), output);
        assertTrue(output.contains("true."), output);
    }

    @Test
    public void list_parser_preserves_supported_list_forms() throws IOException {
        assertEquals("f(\"ab\").", this.parse("f([a,b]).").toString());
        assertEquals("f([a|Tail]).", this.parse("f([a|Tail]).").toString());
        assertEquals("f(\"xy\").", this.parse("f(\"xy\").").toString());
    }
}
