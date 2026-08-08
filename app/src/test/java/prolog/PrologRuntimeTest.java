package prolog;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import prolog.interpreter.FreeVars;
import prolog.interpreter.PrologRuntime;

import java.io.*;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PrologRuntimeTest  extends Tester {
    @AfterEach
    protected void CloseReader() throws IOException {
        super.CloseReader();
    }


    @Test
    void freevars_multiple_removed() throws URISyntaxException, IOException {
        // Arrange
        var list = Arrays.asList(
                new TokenValue(Token.ATOM, "aa"),
                new TokenValue(Token.ATOM, "bb"),
                new TokenValue(Token.ATOM, "cc"),

                new TokenValue(Token.ATOM, "aa"),
                new TokenValue(Token.ATOM, "bb")
                );

        //Action
        var freeList = new FreeVars(list);

        // Assert
        assertEquals(5, list.size());
        assertEquals(3, freeList.size());

    }

    @Test
    public void facts_somefacts_found() throws IOException {
        // Arrange
        Prolog.VERBOSE_LEVEL = 1;
        var line = "color(blue).color(green).";
        var lexer = new Lexer(line);
        var parser = new Parser(lexer);
        var runtime = new PrologRuntime();

        var queryString = "color(blue).";
        var lexerQ = new Lexer(queryString);
        var parserQ = new Parser(lexerQ);
        var query = parser.parse();
        runtime.execute(query.get());
    }

    @Test
    public void multiple_query_variables_are_printed_on_one_line() throws IOException {
        var runtime = new PrologRuntime();
        runtime.setSolutionContinuationReader(() -> '.');

        var output = new ByteArrayOutputStream();
        var previousOut = System.out;
        try {
            System.setOut(new PrintStream(output));
            runtime.execute(this.parse("X = a, Y = b."));
        } finally {
            System.setOut(previousOut);
        }

        assertTrue(output.toString().contains("solution: X = a, Y = b"), output.toString());
    }

    @Test
    public void list_length_recursive_rule_finds_solution() throws IOException {
        var runtime = new PrologRuntime();
        runtime.setSolutionContinuationReader(() -> '.');
        var program = this.parse(
                "list_length([], 0)." +
                "list_length([_|Ls], L) :- list_length(Ls, L0), L is L0 + 1.");
        runtime.consult(program);

        var output = new ByteArrayOutputStream();
        var previousOut = System.out;
        try {
            System.setOut(new PrintStream(output));
            runtime.execute(this.parse("list_length([a], X)."));
        } finally {
            System.setOut(previousOut);
        }

        var solution = output.toString();
        assertTrue(solution.contains("solution: X = 1"));
        assertTrue(!solution.contains("L_"));
        assertTrue(!solution.contains("L0_"));
        assertTrue(!solution.contains("Ls_"));
    }

    @Test
    public void clp_style_greater_than_comparison_filters_rule_body() throws IOException {
        var runtime = new PrologRuntime();
        runtime.setSolutionContinuationReader(() -> '.');
        var program = this.parse(
                "list_length([], 0)." +
                "list_length([_|Ls], L) :- list_length(Ls, L0), L is L0 + 1." +
                "positive_length(List, L) :- list_length(List, L), L #> 0.");
        runtime.consult(program);

        var output = new ByteArrayOutputStream();
        var previousOut = System.out;
        try {
            System.setOut(new PrintStream(output));
            runtime.execute(this.parse("positive_length([a], X)."));
            runtime.execute(this.parse("positive_length([], Y)."));
        } finally {
            System.setOut(previousOut);
        }

        var solution = output.toString();
        assertTrue(solution.contains("solution: X = 1"));
        assertTrue(!solution.contains("Y = "));
    }

    @Test
    public void clp_style_numeric_comparisons_are_supported() throws IOException {
        var runtime = new PrologRuntime();
        runtime.setSolutionContinuationReader(() -> ';');
        var program = this.parse(
                "comparison(ok_gt) :- 2 #> 1." +
                "comparison(ok_gte) :- 2 #>= 2." +
                "comparison(ok_lt) :- 1 #< 2." +
                "comparison(ok_lte) :- 2 #=< 2." +
                "comparison(ok_eq) :- 2 #= 1 + 1." +
                "comparison(ok_neq) :- 2 #\\= 3.");
        runtime.consult(program);

        var output = new ByteArrayOutputStream();
        var previousOut = System.out;
        try {
            System.setOut(new PrintStream(output));
            runtime.execute(this.parse("comparison(X)."));
        } finally {
            System.setOut(previousOut);
        }

        var solution = output.toString();
        assertTrue(solution.contains("X = ok_gt"));
        assertTrue(solution.contains("X = ok_gte"));
        assertTrue(solution.contains("X = ok_lt"));
        assertTrue(solution.contains("X = ok_lte"));
        assertTrue(solution.contains("X = ok_eq"));
        assertTrue(solution.contains("X = ok_neq"));
    }

    @Test
    public void callable_operator_forms_are_supported_for_existing_iso_prolog_built_ins() throws IOException {
        var runtime = new PrologRuntime();
        runtime.setSolutionContinuationReader(() -> ';');
        var program = this.parse(
                "supported(ok_unify) :- =(a, a)." +
                "supported(ok_not_unify) :- \\=(a, b)." +
                "supported(ok_plain_gt) :- >(2, 1)." +
                "supported(ok_plain_gte) :- >=(2, 2)." +
                "supported(ok_plain_lt) :- <(1, 2)." +
                "supported(ok_plain_lte) :- =<(2, 2)." +
                "supported(ok_plain_eq) :- =:=(2, +(1, 1))." +
                "supported(ok_plain_neq) :- =\\=(2, 3)." +
                "supported(ok_gt) :- #>(2, 1)." +
                "supported(ok_gte) :- #>=(2, 2)." +
                "supported(ok_lt) :- #<(1, 2)." +
                "supported(ok_lte) :- #=<(2, 2)." +
                "supported(ok_eq) :- #=(2, +(1, 1))." +
                "supported(ok_neq) :- #\\=(2, 3)." +
                "supported(ok_is) :- is(X, +(1, 1)), #=(X, 2).");
        runtime.consult(program);

        var output = new ByteArrayOutputStream();
        var previousOut = System.out;
        try {
            System.setOut(new PrintStream(output));
            runtime.execute(this.parse("supported(X)."));
        } finally {
            System.setOut(previousOut);
        }

        var solution = output.toString();
        assertTrue(solution.contains("X = ok_unify"), solution);
        assertTrue(solution.contains("X = ok_not_unify"));
        assertTrue(solution.contains("X = ok_plain_gt"));
        assertTrue(solution.contains("X = ok_plain_gte"));
        assertTrue(solution.contains("X = ok_plain_lt"));
        assertTrue(solution.contains("X = ok_plain_lte"));
        assertTrue(solution.contains("X = ok_plain_eq"));
        assertTrue(solution.contains("X = ok_plain_neq"));
        assertTrue(solution.contains("X = ok_gt"));
        assertTrue(solution.contains("X = ok_gte"));
        assertTrue(solution.contains("X = ok_lt"));
        assertTrue(solution.contains("X = ok_lte"));
        assertTrue(solution.contains("X = ok_eq"));
        assertTrue(solution.contains("X = ok_neq"));
        assertTrue(solution.contains("X = ok_is"));
    }

    @Test
    public void callable_unification_failure_prints_false() throws IOException {
        var runtime = new PrologRuntime();
        Prolog.VERBOSE_LEVEL = 1;

        var output = new ByteArrayOutputStream();
        var previousOut = System.out;
        try {
            System.setOut(new PrintStream(output));
            runtime.execute(this.parse("=(a,b)."));
        } finally {
            System.setOut(previousOut);
            Prolog.VERBOSE_LEVEL = 0;
        }

        var solution = output.toString();
        assertTrue(solution.contains("false."));
        assertTrue(!solution.contains("true"));
    }

    @Test
    public void clp_style_comparisons_can_appear_before_recursive_goal() throws IOException {
        var runtime = new PrologRuntime();
        runtime.setSolutionContinuationReader(() -> '.');
        var program = this.parse(
                "list_length([], 0)." +
                "list_length([_|Ls], L) :- L #= L0 + 1, L #> 0, list_length(Ls, L0).");
        runtime.consult(program);

        var output = new ByteArrayOutputStream();
        var previousOut = System.out;
        try {
            System.setOut(new PrintStream(output));
            runtime.execute(this.parse("list_length([a], X)."));
        } finally {
            System.setOut(previousOut);
        }

        assertTrue(output.toString().contains("solution: X = 1"));
    }

    @Test
    public void no_solution_prints_false() throws IOException {
        var runtime = new PrologRuntime();
        var program = this.parse("color(blue).");
        runtime.consult(program);

        var output = new ByteArrayOutputStream();
        var previousOut = System.out;
        try {
            System.setOut(new PrintStream(output));
            runtime.execute(this.parse("color(red)."));
        } finally {
            System.setOut(previousOut);
        }

        assertTrue(output.toString().contains("false."));
    }

    @Test
    public void true_and_false_are_supported_as_iso_prolog_built_ins() throws IOException {
        var runtime = new PrologRuntime();
        runtime.setSolutionContinuationReader(() -> '.');

        var output = new ByteArrayOutputStream();
        var previousOut = System.out;
        try {
            System.setOut(new PrintStream(output));
            runtime.execute(this.parse("true."));
            runtime.execute(this.parse("false."));
        } finally {
            System.setOut(previousOut);
        }

        var result = output.toString();
        assertTrue(result.contains("true."), result);
        assertTrue(result.contains("false."), result);
    }

    @Test
    public void true_and_false_work_inside_compound_queries() throws IOException {
        var runtime = new PrologRuntime();
        runtime.setSolutionContinuationReader(() -> '.');
        runtime.consult(this.parse("color(blue)."));

        var output = new ByteArrayOutputStream();
        var previousOut = System.out;
        try {
            System.setOut(new PrintStream(output));
            runtime.execute(this.parse("true, color(X)."));
            runtime.execute(this.parse("false, color(Y)."));
        } finally {
            System.setOut(previousOut);
        }

        var result = output.toString();
        assertTrue(result.contains("X = blue"), result);
        assertTrue(!result.contains("Y = blue"), result);
        assertTrue(result.contains("false."), result);
    }

    @Test
    public void write_and_nl_are_supported_as_iso_prolog_built_ins() throws IOException {
        var runtime = new PrologRuntime();
        runtime.setSolutionContinuationReader(() -> '.');

        var output = new ByteArrayOutputStream();
        var previousOut = System.out;
        try {
            System.setOut(new PrintStream(output));
            runtime.execute(this.parse("write(hello), nl."));
        } finally {
            System.setOut(previousOut);
        }

        var result = output.toString();
        assertTrue(result.contains("hello" + System.lineSeparator()), result);
        assertTrue(result.contains("true."), result);
    }

    @Test
    public void write_outputs_bound_variables_inside_queries() throws IOException {
        var runtime = new PrologRuntime();
        runtime.setSolutionContinuationReader(() -> '.');
        runtime.consult(this.parse("color(blue)."));

        var output = new ByteArrayOutputStream();
        var previousOut = System.out;
        try {
            System.setOut(new PrintStream(output));
            runtime.execute(this.parse("color(X), write(X), nl."));
        } finally {
            System.setOut(previousOut);
        }

        var result = output.toString();
        assertTrue(result.contains("blue" + System.lineSeparator()), result);
        assertTrue(result.contains("X = blue"), result);
    }

    @Test
    public void iso_term_and_type_built_ins_are_supported() throws IOException {
        var runtime = new PrologRuntime();
        runtime.setSolutionContinuationReader(() -> ';');

        var output = new ByteArrayOutputStream();
        var previousOut = System.out;
        try {
            System.setOut(new PrintStream(output));
            runtime.execute(this.parse("==(a, a), \\==(a, b), @<(a, b), unify_with_occurs_check(X, a), atom(a), integer(1), number(1), atomic(a), compound(f(a)), callable(f(a))."));
        } finally {
            System.setOut(previousOut);
        }

        var result = output.toString();
        assertTrue(result.contains("X = a"), result);
    }

    @Test
    public void iso_term_decomposition_built_ins_are_supported() throws IOException {
        var runtime = new PrologRuntime();
        runtime.setSolutionContinuationReader(() -> '.');

        var output = new ByteArrayOutputStream();
        var previousOut = System.out;
        try {
            System.setOut(new PrintStream(output));
            runtime.execute(this.parse("functor(f(a,b), F, A), arg(2, f(a,b), B), =..(f(a,b), Parts), copy_term(f(X), Copy)."));
        } finally {
            System.setOut(previousOut);
        }

        var result = output.toString();
        assertTrue(result.contains("F = f"), result);
        assertTrue(result.contains("A = 2"), result);
        assertTrue(result.contains("B = b"), result);
        assertTrue(result.replace(" ", "").contains("Parts=[f|[a|[b|[]]]]"), result);
        assertTrue(result.contains("Copy = f("), result);
    }

    @Test
    public void iso_all_solutions_and_atom_built_ins_are_supported() throws IOException {
        var runtime = new PrologRuntime();
        runtime.setSolutionContinuationReader(() -> '.');
        runtime.consult(this.parse("color(red). color(blue)."));

        var output = new ByteArrayOutputStream();
        var previousOut = System.out;
        try {
            System.setOut(new PrintStream(output));
            runtime.execute(this.parse("findall(X, color(X), Xs), atom_length(hello, L), atom_chars(hi, Cs), atom_codes(hi, Codes), char_code(a, Code), number_chars(12, Ns), sub_atom(hello, 1, 3, After, Sub)."));
        } finally {
            System.setOut(previousOut);
        }

        var result = output.toString();
        var compact = result.replace(" ", "");
        assertTrue(compact.contains("Xs=[red|[blue|[]]]"), result);
        assertTrue(result.contains("L = 5"), result);
        assertTrue(compact.contains("Cs=[h|[i|[]]]"), result);
        assertTrue(compact.contains("Codes=[104|[105|[]]]"), result);
        assertTrue(result.contains("Code = 97"), result);
        assertTrue(compact.contains("Ns=[1|[2|[]]]"), result);
        assertTrue(result.contains("After = 1"), result);
        assertTrue(result.contains("Sub = ell"), result);
    }

    @Test
    public void iso_control_and_environment_built_ins_are_supported() throws IOException {
        var runtime = new PrologRuntime();
        runtime.setSolutionContinuationReader(() -> '.');
        runtime.consult(this.parse("color(red). color(blue)."));

        var output = new ByteArrayOutputStream();
        var previousOut = System.out;
        try {
            System.setOut(new PrintStream(output));
            runtime.execute(this.parse("\\+(color(green)), once(color(X)), current_prolog_flag(unknown, Flag), set_prolog_flag(answer, yes), current_prolog_flag(answer, Answer)."));
            runtime.execute(this.parse("op(500, xfy, custom)."));
            runtime.execute(this.parse("current_op(P, Type, custom)."));
        } finally {
            System.setOut(previousOut);
        }

        var result = output.toString();
        assertTrue(result.contains("X = red"), result);
        assertTrue(result.contains("Flag = fail"), result);
        assertTrue(result.contains("Answer = yes"), result);
        assertTrue(result.contains("P = 500"), result);
        assertTrue(result.contains("Type = xfy"), result);
    }

    @Test
    public void iso_listing_and_metadata_built_ins_are_supported() throws IOException {
        var runtime = new PrologRuntime();
        runtime.setSolutionContinuationReader(() -> ';');
        runtime.consult(this.parse(
                "parent(toni,lolo)." +
                "grandparent(X,Y) :- parent(X,Z), parent(Z,Y)."));

        var output = new ByteArrayOutputStream();
        var previousOut = System.out;
        try {
            System.setOut(new PrintStream(output));
            runtime.execute(this.parse("current_predicate(P)."));
            runtime.execute(this.parse("current_op(Priority, xfx, '=')."));
            runtime.execute(this.parse("current_char_conversion(In, Out)."));
        } finally {
            System.setOut(previousOut);
        }

        var compact = output.toString().replace(" ", "");
        assertTrue(compact.contains("P=/(current_predicate,1)"), output.toString());
        assertTrue(compact.contains("P=/(parent,2)"), output.toString());
        assertTrue(compact.contains("P=/(grandparent,2)"), output.toString());
        assertTrue(compact.contains("Priority=700"), output.toString());
        assertTrue(output.toString().contains("false."), output.toString());
    }

    @Test
    public void iso_metadata_predicate_indicator_can_be_queried_directly() throws IOException {
        var runtime = new PrologRuntime();
        runtime.setSolutionContinuationReader(() -> '.');
        runtime.consult(this.parse("parent(toni,lolo)."));

        var output = new ByteArrayOutputStream();
        var previousOut = System.out;
        try {
            System.setOut(new PrintStream(output));
            runtime.execute(this.parse("current_predicate(/(parent, 2))."));
            runtime.execute(this.parse("current_predicate(/(missing, 1))."));
        } finally {
            System.setOut(previousOut);
        }

        var result = output.toString();
        assertTrue(result.contains("true."), result);
        assertTrue(result.contains("false."), result);
    }

    @Test
    public void dot_stops_after_first_solution() throws IOException {
        var runtime = new PrologRuntime();
        runtime.setSolutionContinuationReader(() -> '.');
        var program = this.parse("color(blue).color(green).");
        runtime.consult(program);

        var output = new ByteArrayOutputStream();
        var previousOut = System.out;
        try {
            System.setOut(new PrintStream(output));
            runtime.execute(this.parse("color(X)."));
        } finally {
            System.setOut(previousOut);
        }

        var solution = output.toString();
        assertTrue(solution.contains("solution: X = blue"));
        assertTrue(!solution.contains("X = green"));
    }

    @Test
    public void semicolon_continues_to_next_solution() throws IOException {
        var runtime = new PrologRuntime();
        runtime.setSolutionContinuationReader(() -> ';');
        var program = this.parse("color(blue).color(green).");
        runtime.consult(program);

        var output = new ByteArrayOutputStream();
        var previousOut = System.out;
        try {
            System.setOut(new PrintStream(output));
            runtime.execute(this.parse("color(X)."));
        } finally {
            System.setOut(previousOut);
        }

        var solution = output.toString();
        assertTrue(solution.contains("solution: X = blue"));
        assertTrue(solution.contains("solution: X = green"));
    }

    @Test
    public void asserta_and_assertz_add_facts_in_order() throws IOException {
        var runtime = new PrologRuntime();
        runtime.setSolutionContinuationReader(() -> ';');
        runtime.consult(this.parse("color(blue)."));
        runtime.execute(this.parse("?- assertz(color(green))."));
        runtime.execute(this.parse("?- asserta(color(red))."));

        var output = new ByteArrayOutputStream();
        var previousOut = System.out;
        try {
            System.setOut(new PrintStream(output));
            runtime.execute(this.parse("color(X)."));
        } finally {
            System.setOut(previousOut);
        }

        var solution = output.toString();
        var red = solution.indexOf("X = red");
        var blue = solution.indexOf("X = blue");
        var green = solution.indexOf("X = green");
        assertTrue(red >= 0);
        assertTrue(blue > red);
        assertTrue(green > blue);
    }

    @Test
    public void assert_fails_for_non_ground_fact() throws IOException {
        var runtime = new PrologRuntime();
        runtime.setSolutionContinuationReader(() -> '.');

        var output = new ByteArrayOutputStream();
        var previousOut = System.out;
        try {
            System.setOut(new PrintStream(output));
            runtime.execute(this.parse("assertz(color(X))."));
        } finally {
            System.setOut(previousOut);
        }

        assertTrue(output.toString().contains("false."));
    }

    @Test
    public void assertz_adds_rule() throws IOException {
        var runtime = new PrologRuntime();
        runtime.setSolutionContinuationReader(() -> '.');
        runtime.consult(this.parse("father(toni,lolo)."));
        runtime.execute(this.parse("assertz(:-(parent(X,Y), father(X,Y)))."));

        var output = new ByteArrayOutputStream();
        var previousOut = System.out;
        try {
            System.setOut(new PrintStream(output));
            runtime.execute(this.parse("parent(toni, Who)."));
        } finally {
            System.setOut(previousOut);
        }

        assertTrue(output.toString().contains("Who = lolo"));
    }

    @Test
    public void assertz_adds_grouped_rule() throws IOException {
        var runtime = new PrologRuntime();
        runtime.setSolutionContinuationReader(() -> '.');
        runtime.consult(this.parse(
                "parent(toni,lolo)." +
                "parent(lolo,yannick)."));
        runtime.execute(this.parse("assertz((grandparent(X, Y) :- parent(X, Z), parent(Z, Y)))."));

        var output = new ByteArrayOutputStream();
        var previousOut = System.out;
        try {
            System.setOut(new PrintStream(output));
            runtime.execute(this.parse("grandparent(toni, Who)."));
        } finally {
            System.setOut(previousOut);
        }

        assertTrue(output.toString().contains("Who = yannick"), output.toString());
    }

    @Test
    public void asserta_and_assertz_add_rules_in_order() throws IOException {
        var runtime = new PrologRuntime();
        runtime.setSolutionContinuationReader(() -> ';');
        runtime.consult(this.parse(
                "kind(static). " +
                "source(static)."));
        runtime.execute(this.parse("assertz(:-(source(z_last), kind(static)))."));
        runtime.execute(this.parse("asserta(:-(source(a_first), kind(static)))."));

        var output = new ByteArrayOutputStream();
        var previousOut = System.out;
        try {
            System.setOut(new PrintStream(output));
            runtime.execute(this.parse("source(X)."));
        } finally {
            System.setOut(previousOut);
        }

        var solution = output.toString();
        var first = solution.indexOf("X = a_first");
        var statik = solution.indexOf("X = static");
        var last = solution.indexOf("X = z_last");
        assertTrue(first >= 0);
        assertTrue(statik > first);
        assertTrue(last > statik);
    }

    @Test
    public void consult_loads_file_from_query() throws IOException {
        var programFile = Files.createTempFile("myo-prolog-consult", ".pl");
        Files.writeString(programFile,
                "father(toni,lolo)." +
                "parent(X,Y) :- father(X,Y).");

        var runtime = new PrologRuntime();
        runtime.setSolutionContinuationReader(() -> '.');
        runtime.execute(this.parse("consult('" + programFile + "')."));

        var output = new ByteArrayOutputStream();
        var previousOut = System.out;
        try {
            System.setOut(new PrintStream(output));
            runtime.execute(this.parse("parent(toni, Who)."));
        } finally {
            System.setOut(previousOut);
            Files.deleteIfExists(programFile);
        }

        assertTrue(output.toString().contains("Who = lolo"));
    }

    @Test
    public void consult_can_continue_with_remaining_goals() throws IOException {
        var programFile = Files.createTempFile("myo-prolog-consult-tail", ".pl");
        Files.writeString(programFile, "color(blue).");

        var runtime = new PrologRuntime();
        runtime.setSolutionContinuationReader(() -> '.');

        var output = new ByteArrayOutputStream();
        var previousOut = System.out;
        try {
            System.setOut(new PrintStream(output));
            runtime.execute(this.parse("consult('" + programFile + "'), color(X)."));
        } finally {
            System.setOut(previousOut);
            Files.deleteIfExists(programFile);
        }

        assertTrue(output.toString().contains("X = blue"), output.toString());
    }

}
