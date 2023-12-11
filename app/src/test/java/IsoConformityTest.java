import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;
import prolog.Lexer;
import prolog.Parser;

import java.io.*;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

public class IsoConformityTest extends Tester {


    @AfterEach
    void CloseReader() throws IOException {
        super.CloseReader();
    }

    private boolean shouldBeError(String codex) {
        return codex.contains("syntax err") ||codex.contains("waits");
    }

    @ParameterizedTest
    @CsvFileSource(resources = "/tests/iso-conformity-testing.csv", numLinesToSkip = 1)
    //"no","Query","Codex","IF","SWI","YAP","B","GNU","SICStus","Minerva","XSB","Ciao","IV","ECLiPSe","Scryer"
    void test_all(String no, String query, String codex, String sol2, String sol3, String sol4, String sol5, String sol6, String sol7, String sol8, String sol9, String sol10, String sol11, String sol12, String sol13 ) throws URISyntaxException, IOException {
        var adaptedQuery = query;//.replaceAll("\\n", "\r\n").replaceAll("\\t", ""+(char)9);
        // Arrange
        var reader = new StringReader(adaptedQuery);
        var lexer = new Lexer(reader);
        var parser = new Parser(lexer);

        try {
            System.out.println("Parse: " + adaptedQuery);
            var program = parser.parse();
            if (this.shouldBeError(codex)) {
                assertFalse(true, "Error should occur on "+query+" but was parsed");
            }

            //Action
            var parsedLines = program.get();
            var str = parsedLines.toString();

            assertNotNull(parsedLines);
            assertNotNull(str);
            assertTrue(str.length() > 0);
        } catch (Exception e) {
            if (this.shouldBeError(codex)) {
                System.out.println("Correct failure: "+e.getMessage());
                assertNotNull(e);
            } else {
                System.out.println("Error occured on "+query+" but should not ");
                throw e;
            }
        }
    }
}
