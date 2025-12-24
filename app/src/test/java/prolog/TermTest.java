package prolog;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import prolog.interpreter.PrologRuntime;

import java.io.*;
import java.net.URISyntaxException;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TermTest  extends Tester {
    @AfterEach
    protected void CloseReader() throws IOException {
        super.CloseReader();
    }



    @Test
    public void facts_somefacts_found() throws IOException {
        // Arrange
        Prolog.VERBOSE_LEVEL = 1;
        var runtime = new PrologRuntime();

        var line = "color(blue).color(green).";
        var facts = this.consult(runtime, line);

        var queryString = "color(X).";
        var query = this.parse(queryString);

        runtime.execute(query);

    }


    @Test
    public void execute_childrenparent_found() throws IOException {
        // Arrange
        Prolog.VERBOSE_LEVEL = 1;
        var runtime = new PrologRuntime();

        var line = "child(silvan, lorenz). child(yannick, lorenz). parent(X,Y) :- child(Y, X).";
        var facts = this.consult(runtime, line);

        var queryString = "parent(A,B).";
        var query = this.parse(queryString);

        runtime.execute(query);

    }

    @Test()
    public void execute_paths_found() throws IOException, URISyntaxException {
        // Arrange
        Prolog.VERBOSE_LEVEL = 1;
        var runtime = new PrologRuntime();

        ReadReader("path.pl");
        var facts = this.consult(runtime, reader);

        var queryString = "path(a,c,A).";
        var query = this.parse(queryString);

        runtime.execute(query);

    }

    @Test()
    public void asTerms_rule_found() throws IOException, URISyntaxException {
        // Arrange
        Prolog.VERBOSE_LEVEL = 1;

        var line ="path_helper(X, Y, f([Z|X])) :- successor(X, Z), path_helper(Z, Y, f(f(f(Path))) ).";
        var program = this.parse(line);
        var rule = program.clauses.get(0);

        // Action
        var terms = rule.asTerms();
        var lhs = terms.lhs();
        var rhs = terms.rhs();

        // Assertions
        // X Y Z Path
        assertEquals(4, terms.freevars().size());
        assertEquals("X", terms.freevars().get(0).toValueString());
        assertEquals("Y", terms.freevars().get(1).toValueString());
        assertEquals("Z", terms.freevars().get(2).toValueString());
        assertEquals("Path", terms.freevars().get(3).toValueString());

        assertEquals(3, lhs.freevars().size());
        assertEquals(4, rhs.freevars().size());

    }

}
