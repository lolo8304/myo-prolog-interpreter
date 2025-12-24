package prolog;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.net.URISyntaxException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class TermStatusTest  extends Tester {
    @AfterEach
    protected void CloseReader() throws IOException {
        super.CloseReader();
    }


    @Test void test() throws URISyntaxException, IOException {
        // Arrange
        ReadReader("valid-term-status.txt");

        while (reader.ready()) {
            var line = reader.readLine();
            if (!line.equals("")) {
                System.out.println(line);

                // Arrange
                var parser = this.parser(line);
                var program = parser.parse().get();
                System.out.println(program);
                var realComment = parser.comments().isEmpty() ? "%" : parser.comments().get(0).toValueString();
                var splitComent = realComment.split("%");
                var comment = splitComent.length > 0 ? splitComent[1].trim() : "";
                var expectedG = comment.contains("G");
                var expectedP = comment.contains("P");
                var expectedI = comment.contains("I");
                var expectedU = comment.contains("U");

                // Action
                var G = program.isGround();
                var P = program.isPartiallyInstantiated();
                var I = program.isInstantiated();
                var U = program.isUnInstantiated();

                // Assert
                assertNotNull(program, line);
                assertEquals(expectedG, G, "G of "+ line);
                assertEquals(expectedP, P, "P of "+ line);
                assertEquals(expectedI, I, "I of "+ line);
                assertEquals(expectedU, U, "U of "+ line);
            }
        }

    }

}
