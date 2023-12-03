package prolog;

import prolog.nodes.FactNode;
import prolog.nodes.ProgramNode;

import java.io.IOException;

public class PrologRuntime {

    public final Memory memory;

    public PrologRuntime() {
        this.memory = new Memory(this);
    }

    public void execute(ProgramNode program) throws IOException {
        program.execute(this);
    }

    public void findSolution(FactNode factNode) {

    }

    public static class Context {
        public Context(PrologRuntime runtime) {

        }

        public void findSolution(FactNode factNode) {

        }
    }


}
