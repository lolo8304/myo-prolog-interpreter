package prolog;

import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.UserInterruptException;
import org.jline.reader.impl.history.DefaultHistory;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import prolog.interpreter.PrologRuntime;
import prolog.nodes.ClauseNode;
import prolog.nodes.ProgramNode;

import java.io.*;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Optional;
import java.util.Scanner;
import java.util.Set;

public class PrologCli {

    private static final String QUERY_PROMPT = "?- ";
    private static final String USER_CONSULT_PROMPT = "|: ";
    private static final String USER_CONSULT_COMMAND = "consult(user).";
    private static final String USER_RECONSULT_COMMAND = "reconsult(user).";
    private static final Path HISTORY_FILE = Path.of(System.getProperty("user.home"), ".prolog_history");

    private final PrologRuntime runtime;
    private final Scanner scanner;
    private final boolean loadInitFile;
    private final boolean historyEnabled;
    private final LineReader lineReader;
    private Terminal terminal;

    public PrologCli() {
        this(true);
    }

    public PrologCli(boolean loadInitFile) {
        this(loadInitFile, true);
    }

    public PrologCli(boolean loadInitFile, boolean historyEnabled) {
        this.runtime = new PrologRuntime();
        this.scanner = new Scanner(System.in);
        this.loadInitFile = loadInitFile;
        this.historyEnabled = historyEnabled;
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
            var builder = LineReaderBuilder.builder()
                    .terminal(terminal)
                    .history(new DefaultHistory());
            if (this.historyEnabled) {
                builder.variable(LineReader.HISTORY_FILE, HISTORY_FILE);
            }
            return builder.build();
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
            if (this.isUserConsultCommand(input.get())) {
                this.executeUserConsult(false);
            } else if (this.isUserReconsultCommand(input.get())) {
                this.executeUserConsult(true);
            } else {
                this.execute(new StringReader(input.get()));
            }
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
                return Optional.of(this.lineReader.readLine(QUERY_PROMPT));
            } catch (EndOfFileException | UserInterruptException e) {
                return Optional.empty();
            }
        }

        System.out.print(QUERY_PROMPT);
        if (scanner.hasNextLine()) {
            return Optional.of(scanner.nextLine());
        } else {
            return Optional.empty();
        }
    }

    private Optional<String> readFromUserConsultConsole() throws IOException {
        if (this.lineReader != null) {
            try {
                return Optional.of(this.lineReader.readLine(USER_CONSULT_PROMPT));
            } catch (EndOfFileException | UserInterruptException e) {
                return Optional.empty();
            }
        }

        System.out.print(USER_CONSULT_PROMPT);
        if (scanner.hasNextLine()) {
            return Optional.of(scanner.nextLine());
        } else {
            return Optional.empty();
        }
    }

    private boolean isUserConsultCommand(String input) {
        var command = input.trim();
        if (command.startsWith(QUERY_PROMPT.trim())) {
            command = command.substring(QUERY_PROMPT.trim().length()).trim();
        }
        return command.equals(USER_CONSULT_COMMAND);
    }

    private boolean isUserReconsultCommand(String input) {
        var command = input.trim();
        if (command.startsWith(QUERY_PROMPT.trim())) {
            command = command.substring(QUERY_PROMPT.trim().length()).trim();
        }
        return command.equals(USER_RECONSULT_COMMAND);
    }

    private void executeUserConsult(boolean overwrite) throws IOException {
        System.out.println("Press Ctrl-D to stop user mode and go back to query mode.");
        var overwrittenPredicateIndicators = new HashSet<String>();
        var input = this.readFromUserConsultConsole();
        while (input.isPresent()) {
            this.executeUserConsult(new StringReader(input.get()), overwrite, overwrittenPredicateIndicators);
            input = this.readFromUserConsultConsole();
        }
    }

    void executeUserConsult(Reader reader) throws IOException {
        this.executeUserConsult(reader, false, new HashSet<>());
    }

    void executeUserConsult(Reader reader, boolean overwrite) throws IOException {
        this.executeUserConsult(reader, overwrite, new HashSet<>());
    }

    private void executeUserConsult(Reader reader, boolean overwrite, Set<String> overwrittenPredicateIndicators) throws IOException {
        this.runtime.consultingModeOn();
        try {
            var lexer = new Lexer(reader);
            var parser = new Parser(lexer);
            var program = parser.parse();
            while (program.isPresent()) {
                if (overwrite) {
                    this.removeExistingClausesOnce(program.get(), overwrittenPredicateIndicators);
                }
                this.runtime.execute(program.get());
                program = parser.parse();
            }
        } catch (IOException e) {
            System.out.println("Error: " + e.getMessage());
        } finally {
            this.runtime.consultingModeOff();
        }
    }

    private void removeExistingClausesOnce(ProgramNode program, Set<String> overwrittenPredicateIndicators) {
        for (var clause: program.clauses) {
            var predicateIndicator = this.predicateIndicator(clause);
            if (predicateIndicator.isPresent() && overwrittenPredicateIndicators.add(predicateIndicator.get())) {
                this.runtime.top().memory.removeClauses(predicateIndicator.get());
            }
        }
    }

    private Optional<String> predicateIndicator(ClauseNode clause) {
        if (clause.fact != null) {
            return Optional.of(clause.fact.predicateIndicator());
        }
        if (clause.rule != null) {
            return Optional.of(clause.rule.head.predicateIndicator());
        }
        return Optional.empty();
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
