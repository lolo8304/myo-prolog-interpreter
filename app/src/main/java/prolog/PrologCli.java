package prolog;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.Optional;
import java.util.Scanner;

public class PrologCli {

    private final PrologRuntime runtime;

    public PrologCli() {
        this.runtime = new PrologRuntime();
    }

    public PrologCli parse(Reader reader) throws IOException {
        var lexer = new Lexer(reader);
        var parser = new Parser(lexer);
        var program = parser.parse();
        while (program.isPresent()) {
            program.get().execute(this.runtime);
            program = parser.parse();
        }
        return this;
    }


    public void execute(Reader reader) throws IOException {
        try {
            this.parse(reader);
        } catch (IOException e) {
            System.out.println("Error: "+e.getMessage());
        }
    }

    public void execute() throws IOException {
        var input = this.readFromConsole();
        while (input.isPresent() && !input.get().equalsIgnoreCase("quit")) {
            this.execute(new StringReader(input.get()));
            input = this.readFromConsole();
        }
    }



    private Optional<String> readFromConsole() throws IOException {
        while (System.in.available() > 0) {
            System.in.read();
        }
        System.out.print("?- ");
        Scanner scanner = new Scanner(System.in);
        if (scanner.hasNext()) {
            return Optional.of(scanner.nextLine());
        } else {
            return Optional.empty();
        }
    }
}
