package prolog;

import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.UserInterruptException;
import org.jline.reader.impl.history.DefaultHistory;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import prolog.interpreter.PrologRuntime;

import java.io.*;
import java.util.Optional;
import java.util.Scanner;

public class PrologCli {

    private final PrologRuntime runtime;
    private final Scanner scanner;
    private final boolean loadInitFile;
    private final LineReader lineReader;
    private Terminal terminal;

    public PrologCli() {
        this(true);
    }

    public PrologCli(boolean loadInitFile) {
        this.runtime = new PrologRuntime();
        this.scanner = new Scanner(System.in);
        this.loadInitFile = loadInitFile;
        this.lineReader = this.createLineReader();
        this.runtime.setSolutionContinuationReader(this::readSolutionContinuation);
    }

    private LineReader createLineReader() {
        if (System.console() == null) {
            return null;
        }

        try {
            Terminal terminal = TerminalBuilder.builder()
                    .system(true)
                    .build();
            this.terminal = terminal;
            return LineReaderBuilder.builder()
                    .terminal(terminal)
                    .history(new DefaultHistory())
                    .build();
        } catch (IOException e) {
            return null;
        }
    }

    public PrologCli consult(Reader reader) throws IOException {
        this.runtime.consult(reader);
        return this;
    }

    public PrologCli executeProgram(Reader reader) throws IOException {
        var lexer = new Lexer(reader);
        var parser = new Parser(lexer);
        var program = parser.parse();
        while (program.isPresent()) {
            this.runtime.execute(program.get());
            program = parser.parse();
        }
        return this;
    }


    public void execute(Reader reader) throws IOException {
        try {
            this.executeProgram(reader);
        } catch (IOException e) {
            System.out.println("Error: "+e.getMessage());
        }
    }

    public void execute() throws IOException {
        if (this.loadInitFile) {
            var initFile = this.findInitFile();
            if (initFile.isPresent()) {
                try (var r = new FileReader(initFile.get())) {
                    this.runtime.setSolutionContinuationReader(() -> '.');
                    try {
                        this.consult(r);
                    } finally {
                        this.runtime.setSolutionContinuationReader(this::readSolutionContinuation);
                    }
                }
            }
        }
        var input = this.readFromConsole();
        while (input.isPresent() && !input.get().equalsIgnoreCase("quit")) {
            this.execute(new StringReader(input.get()));
            input = this.readFromConsole();
        }
    }


    private Optional<File> findInitFile() {
        var current = new File("init.pl");
        if (current.exists()) {
            return Optional.of(current);
        }

        var parent = new File("..", "init.pl");
        if (parent.exists()) {
            return Optional.of(parent);
        }

        return Optional.empty();
    }



    private Optional<String> readFromConsole() throws IOException {
        if (this.lineReader != null) {
            try {
                return Optional.of(this.lineReader.readLine("?- "));
            } catch (EndOfFileException | UserInterruptException e) {
                return Optional.empty();
            }
        }

        System.out.print("?- ");
        if (scanner.hasNextLine()) {
            return Optional.of(scanner.nextLine());
        } else {
            return Optional.empty();
        }
    }

    private char readSolutionContinuation() {
        if (this.terminal != null) {
            var previousAttributes = this.terminal.enterRawMode();
            try {
                var input = this.terminal.reader().read();
                if (input < 0) {
                    return '.';
                }

                System.out.println(input == ' ' ? ";" : String.valueOf((char) input));
                return input == ';' || input == ' ' ? ';' : '.';
            } catch (IOException e) {
                return '.';
            } finally {
                this.terminal.setAttributes(previousAttributes);
            }
        }

        if (!this.scanner.hasNextLine()) {
            return '.';
        }

        var input = this.scanner.nextLine().trim();
        if (input.equals(";")) {
            return ';';
        }
        return '.';
    }
}
