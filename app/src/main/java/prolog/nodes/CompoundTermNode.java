package prolog.nodes;

import prolog.PrologRuntime;
import prolog.Token;
import prolog.TokenValue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static prolog.nodes.ArgumentNode.NIL_ARGUMENT;

public class CompoundTermNode extends CompoundNode {
    public final List<ArgumentNode> arguments;

    public CompoundTermNode(TokenValue functor) {
        super(functor);
        this.arguments = new ArrayList<>();
    }

    @Override
    public List<ArgumentNode> arguments() {
        return this.arguments;
    }

    public CompoundTermNode(TokenValue functor, List<ArgumentNode> arguments) {
        super(functor);
        this.arguments = arguments;
        this.key();
    }

    public CompoundTermNode addArgument(ArgumentNode argument) {
        this.arguments.add(argument);
        this.resetKey();
        return this;
    }


}
