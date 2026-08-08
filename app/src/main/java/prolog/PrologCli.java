package prolog;

import prolog.interpreter.PrologRuntime;

import java.io.*;
import java.util.Optional;
import java.util.Scanner;

public class PrologCli {

    private final PrologRuntime runtime;
    private final Scanner scanner;

    public PrologCli() {
        this.runtime = new PrologRuntime();
        this.scanner = new Scanner(System.in);
        this.runtime.setSolutionContinuationReader(this::readSolutionContinuation);
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
        System.out.print("?- ");
        if (scanner.hasNextLine()) {
            return Optional.of(scanner.nextLine());
        } else {
            return Optional.empty();
        }
    }

    private char readSolutionContinuation() {
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
