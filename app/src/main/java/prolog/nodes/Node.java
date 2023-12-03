package prolog.nodes;

import prolog.Memory;
import prolog.PrologRuntime;

import java.io.IOException;

public interface Node {
    void execute(PrologRuntime runtime) throws IOException;
    StringBuilder append(StringBuilder builder);
}
