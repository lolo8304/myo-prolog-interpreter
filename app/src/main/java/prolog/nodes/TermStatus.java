package prolog.nodes;

import prolog.TokenValue;
import prolog.interpreter.PrologRuntime;

import java.io.IOException;
import java.util.List;

public interface TermStatus {
    // is a term not var, not atom
    boolean isGround();

    // if a term inside tree at least 1 that is UnInstantiated
    boolean isPartiallyInstantiated();

    // if a variable is solved or an atom, else false
    boolean isInstantiated();

    // if variable is not solved, else false
    boolean isUnInstantiated();

}
