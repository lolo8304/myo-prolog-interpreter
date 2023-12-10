import org.junit.jupiter.api.AfterEach;
import prolog.Lexer;
import prolog.Parser;
import prolog.interpreter.PrologRuntime;
import prolog.nodes.ProgramNode;

import java.io.*;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;

public abstract class Tester {


    protected BufferedReader reader;

    void ReadReader(String testfile) throws FileNotFoundException, URISyntaxException {
        URL resource = TermStatusTest.class.getResource("tests/"+testfile);
        File file = Paths.get(resource.toURI()).toFile();
        reader = new BufferedReader(new FileReader(file));
    }

    void CloseReader() throws IOException {
        if (reader != null) {
            reader.close();
        }
    }


    protected ProgramNode consult(PrologRuntime runtime, String line) throws IOException {
        var facts = this.parse(line);
        runtime.consult(facts);
        return facts;
    }
    protected ProgramNode parse(String line) throws IOException {
        return this.parser(line).parse().get();
    }
    protected Parser parser(String line) throws IOException {
        var lexer = new Lexer(line);
        return new Parser(lexer);
    }
}
