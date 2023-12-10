package prolog.nodes;

import prolog.TokenValue;
import prolog.interpreter.*;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

public abstract class AbstractNode implements Node, TermStatus {

    @Override
    public String toString() {
        return this.append(new StringBuilder()).toString();
    }

    @Override
    public FreeVars freevars() {
        return TokenValue.EMPTY_LIST;
    }

    @Override
    public abstract boolean isGround();

    @Override
    public abstract boolean isPartiallyInstantiated();

    @Override
    public abstract boolean isInstantiated();

    @Override
    public abstract boolean isUnInstantiated();


    public abstract Optional<Constr> asConstr();

    public Optional<Var> asVars() {
        return Optional.empty();
    }

    public Term asTerm() {
        var asVar = this.asVars();
        if (asVar.isPresent()) return asVar.get();
        return this.asConstr().get();
    };

}
