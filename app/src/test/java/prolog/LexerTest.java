package prolog;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LexerTest extends Tester {

    static Stream<String> validTokenLines() throws IOException, URISyntaxException {
        var r = ReadReaderS("valid-tokens.txt");
        try {
            var lines = r.lines().toList();
            return lines.stream().filter(line -> !line.isBlank());
        } finally {
            r.close();
        }
    }

    @ParameterizedTest
    @MethodSource("validTokenLines")
    void testValidTokenLine(String line) throws IOException {
        var lexer = new Lexer(line);
        var tokens = lexer.tokens();
        System.out.println(tokens.toString());
        assertNotNull(tokens);
        assertTrue(tokens.size() > 0);
    }
}