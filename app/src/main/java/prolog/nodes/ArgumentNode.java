package prolog.nodes;

import prolog.interpreter.*;
import prolog.Token;
import prolog.TokenValue;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class ArgumentNode extends AbstractNode implements Term  {

    public static ArgumentNode NIL_ARGUMENT;
    static {
        try {
            NIL_ARGUMENT = new ArgumentNode(TokenValue.NIL);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public final CompoundNode compoundTerm;
    public final TokenValue atom;
    public final TokenValue variable;
    public final TokenValue number;

    public ArgumentNode(CompoundNode compoundTerm) {
        this.compoundTerm = compoundTerm;
        this.atom = null;
        this.variable = null;
        this.number = null;
    }
    public ArgumentNode(TokenValue token) throws IOException {
        this.compoundTerm = null;
        if (token.is(Token.VARIABLE, Token.ANONYMOUS_VARIABLE)) {
            this.variable = token;
            this.number = null;
            this.atom = null;
        } else if (token.is(Token.NUMBER)) {
            this.variable = null;
            this.number = token;
            this.atom = null;
        } else if (token.is(Token.ATOM, Token.QUOTED_ATOM, Token.nil)) {
            this.variable = null;
            this.number = null;
            this.atom = token;
        } else {
            throw new IOException("Illegal token '"+token+"' used for argument");
        }
    }

    public String key() {
        if (this.variable != null) return this.variable.toValueString();
        if (this.atom != null) return this.atom.toValueString();
        if (this.number != null) return this.number.toValueString();
        return this.compoundTerm.key();
    }


    @Override
    public StringBuilder append(StringBuilder builder) {
        if (this.variable != null) return builder.append(this.variable.toValueString());
        if (this.atom != null) return builder.append(this.atom.toValueString());
        if (this.number != null) return builder.append(this.number.toValueString());
        return this.compoundTerm.append(builder);
    }

    private TermStatus termStatus() {
        if (this.variable != null) return this.variable;
        if (this.atom != null) return this.atom;
        if (this.number != null) return this.number;
        return this.compoundTerm;
    }

    @Override
    public boolean isGround() {
        return this.termStatus().isGround();
    }

    @Override
    public boolean isPartiallyInstantiated() {
        return this.termStatus().isPartiallyInstantiated();
    }

    @Override
    public boolean isInstantiated() {
        return this.termStatus().isInstantiated();
    }

    @Override
    public boolean isUnInstantiated() {
        if (this.compoundTerm != null) return false;
        return this.termStatus().isUnInstantiated();
    }

    public List<ArgumentNode> arguments() {
        if (this.compoundTerm != null) return this.compoundTerm.arguments();
        return List.of(this);
    }

    public boolean isChars() {
        if (this.atom != null) {
            return this.atom == TokenValue.NIL || this.atom.string.length() == 1;
        } else
            return this.compoundTerm != null && this.compoundTerm.isChars();
    }

    @Override
    public FreeVars freevars() {
        if (this.variable != null) {
            return FreeVars.of(this.variable);
        } else if (this.compoundTerm != null) {
            return this.compoundTerm.freevars();
        } else {
            return super.freevars();
        }
    }

    @Override
    public Term map(Subst s) {
        if (this.atom != null || this.number != null) return this;
        if (this.compoundTerm != null) return this.compoundTerm.map(s);
        return this.variable.map(s);
    }

    @Override
    public Optional<Subst> pmatch(Term term, Subst s) {
        if (this.atom != null) return this.atom.pmatch(term, s);
        if (this.number != null) return this.number.pmatch(term,s);
        if (this.compoundTerm != null) return this.compoundTerm.pmatch(term, s);
        return this.variable.pmatch(term, s);
    }

    @Override
    public Optional<Subst> unify(Term y, Subst s) {
        if (this.atom != null) return this.atom.unify(y, s);
        if (this.number != null) return this.number.unify(y, s);
        if (this.compoundTerm != null) return this.compoundTerm.unify(y, s);
        return this.variable.unify(y, s);
    }

    @Override
    public Optional<Constr> asConstr() {
        if (this.compoundTerm != null) return this.compoundTerm.asConstr();
        return Optional.empty();
    }

    @Override
    public Optional<Var> asVar() {
        if (this.variable != null) return this.variable.asVar();
        return Optional.empty();
    }

    public PredicateNode asPredicate() {
        if (this.compoundTerm != null) return this.compoundTerm.asPredicate();
        if (this.atom != null) return new PredicateNode(this.atom);
        if (this.variable != null) return new PredicateNode(this.variable);
        return new PredicateNode(this.number);
    }

    @Override
    public Term asTerm() {
        if (this.compoundTerm != null) return this.compoundTerm.asConstr().get();
        if (this.atom != null) return this.atom;
        if (this.variable != null) return new Var(this.variable);
        return this.number;
    }

    @Override
    public Terms asTerms() {
        return this.asPredicate().asTerms();
    }
}
