package prolog;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import prolog.interpreter.FreeVars;
import prolog.interpreter.PrologRuntime;

import java.io.*;
import java.net.URISyntaxException;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;

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

}
