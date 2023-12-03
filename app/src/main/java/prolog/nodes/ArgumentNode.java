package prolog.nodes;

import prolog.Memory;
import prolog.PrologRuntime;
import prolog.Token;
import prolog.TokenValue;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class ArgumentNode extends AbstractNode {

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
    public void execute(PrologRuntime runtime) {

    }

    @Override
    public StringBuilder append(StringBuilder builder) {
        if (this.variable != null) return builder.append(this.variable.toValueString());
        if (this.atom != null) return builder.append(this.atom.toValueString());
        if (this.number != null) return builder.append(this.number.toValueString());
        return this.compoundTerm.append(builder);
    }

    public boolean isGround() {
        return (this.atom !=null || this.number != null) || this.compoundTerm.isGround();
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
}
