package prolog;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.net.URISyntaxException;

import static org.junit.jupiter.api.Assertions.*;

public class ListsTest  extends Tester {
    @AfterEach
    protected void CloseReader() throws IOException {
        super.CloseReader();
    }

    @Test
    void isChars_string_ok() throws URISyntaxException, IOException {
        // Arrange
        var reader = new StringReader("f([a,b,c,d]).");
        var lexer = new Lexer(reader);
        var parser = new Parser(lexer);
        var program = parser.parse();

        //Action
        var list = program.get().clauses.get(0).fact.predicate.arguments.get(0);
        var str = list.toString();

        // Assert
        assertEquals("\"abcd\"", str);

    }

    @Test
    void isChars_stringWithAdditionalEmpty_ok() throws URISyntaxException, IOException {
        // Arrange
        var reader = new StringReader("f([a,b,c,d,[]]).");
        var lexer = new Lexer(reader);
        var parser = new Parser(lexer);
        var program = parser.parse();

        //Action
        var list = program.get().clauses.get(0).fact.predicate.arguments.get(0);
        var str = list.toString();

        // Assert
        assertEquals("\"abcd\"", str);

    }

    @Test
    void isChars_stringTailsAB_ok() throws URISyntaxException, IOException {

        String[] lines = {
                "f([a,b]).",
                "f([a|[b]]).",
                "f([a|[b|[]]]).",
        };
        for (var line: lines) {
            // Arrange
            var reader = new StringReader(line);
            var lexer = new Lexer(reader);
            var parser = new Parser(lexer);
            var program = parser.parse();

            //Action
            var list = program.get();
            var str = list.toString();

            // Assert
            assertEquals("f(\"ab\").", str);
        }
    }

    @Test
    void isChars_stringTailsABC_ok() throws URISyntaxException, IOException {

        String[] lines = {
                "f([a,b,c|[]]).",
                "f([a,b|[c]]).",
                "f([a|[b,c]]).",
                "f([a|[b|[c]]]).",
                "f([a|[b|[c|[]]]]).",
        };
        for (var line: lines) {
            // Arrange
            var reader = new StringReader(line);
            var lexer = new Lexer(reader);
            var parser = new Parser(lexer);
            var program = parser.parse();

            //Action
            var list = program.get();
            var str = list.toString();

            // Assert
            assertEquals("f(\"abc\").", str);
        }
    }


    @Test
    void isChars_charcodes_ok() throws URISyntaxException, IOException {

        String[] lines = {
                "f(0''').",
                "f(0'\\').",
        };
        for (var line: lines) {
            // Arrange
            var reader = new StringReader(line);
            var lexer = new Lexer(reader);
            var parser = new Parser(lexer);
            var program = parser.parse();

            //Action
            var list = program.get();
            var str = list.toString();

            // Assert
            assertEquals("f(39).", str);
        }
    }
}
