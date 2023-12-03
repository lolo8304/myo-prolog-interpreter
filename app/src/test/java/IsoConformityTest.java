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

public class IsoConformityTest {

    private BufferedReader reader;

    void ReadReader(String testfile) throws FileNotFoundException, URISyntaxException {
        URL resource = IsoConformityTest.class.getResource("tests/"+testfile);
        File file = Paths.get(resource.toURI()).toFile();
        reader = new BufferedReader(new FileReader(file));
    }

    @AfterEach
    void CloseReader() throws IOException {
        if (reader != null) {
            reader.close();
        }
    }

    @ParameterizedTest
    @CsvFileSource(resources = "/tests/iso-conformity-testing.csv", numLinesToSkip = 1)
    //"no","Query","Codex","IF","SWI","YAP","B","GNU","SICStus","Minerva","XSB","Ciao","IV","ECLiPSe","Scryer"
    void test_all(String no, String query, String sol1, String sol2, String sol3, String sol4, String sol5, String sol6, String sol7, String sol8, String sol9, String sol10, String sol11, String sol12, String sol13 ) throws URISyntaxException, IOException {
        var adaptedQuery = query.replaceAll("\\n", "\r\n").replaceAll("\\t", "\t");
        // Arrange
        var reader = new StringReader(adaptedQuery);
        var lexer = new Lexer(reader);
        var parser = new Parser(lexer);

        System.out.println("Parse: "+adaptedQuery);
        var program = parser.parse();

        //Action
        var parsedLines = program.get();
        var str = parsedLines.toString();

        assertNotNull(parsedLines);
        assertNotNull(str);
        assertTrue(str.length() > 0);
    }
}
