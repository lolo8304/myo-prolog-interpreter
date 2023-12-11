package prolog.nodes;

import prolog.TokenValue;
import prolog.interpreter.PrologRuntime;

import java.io.IOException;
import java.util.List;

public interface Node {
    StringBuilder append(StringBuilder builder);
    List<TokenValue> freevars();
}
