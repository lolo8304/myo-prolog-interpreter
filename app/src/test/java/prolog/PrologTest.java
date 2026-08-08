package prolog;

import org.junit.jupiter.api.Test;
import picocli.CommandLine;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class PrologTest {

    @Test
    public void no_history_option_is_recognized() {
        var prolog = new Prolog();
        new CommandLine(prolog).parseArgs("--no-history");

        assertTrue(prolog.noHistory);
    }
}
