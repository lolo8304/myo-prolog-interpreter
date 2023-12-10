import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import prolog.*;
import prolog.interpreter.FreeVars;
import prolog.interpreter.PrologRuntime;
import prolog.nodes.ProgramNode;

import java.io.*;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TermTest  extends Tester {
    @AfterEach
    void CloseReader() throws IOException {
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

        var queryString = "parent(X,Y).";
        var query = this.parse(queryString);

        runtime.execute(query);

    }

}
