package prolog;

import prolog.interpreter.*;
import prolog.nodes.TermStatus;

import java.util.*;
import java.util.stream.Stream;

public class TokenValue implements Term, TermStatus {

    public static TokenValue NIL = new TokenValue(Token.nil, "[]");
    public static TokenValue FALSE = new TokenValue(Token.ATOM, "false");
    public static TokenValue TRUE = new TokenValue(Token.ATOM, "true");
    public static FreeVars EMPTY_LIST = new FreeVars();

    public final Token token;
    public final Object value;
    public final String string;

    public TokenValue(Token token) {
        this(token, null);
    }
    public TokenValue(Token token, Object value) {
        this.token = token;
        this.value = value;
        this.string = value != null ? value.toString() : "";
    }

    @Override
    public String toString() {
        return this.append(new StringBuilder()).toString();
    }
    public String toValueString() {
        return this.string;
    }

    public boolean isNotAnyOf(Token... tokens) {
        return !this.is(tokens);
    }

    public boolean is(Token... tokens) {
        if (tokens == null || tokens.length == 0) return false;
        for (int i = 0; i < tokens.length; i++) {
            if (this.token == tokens[i]) {
                return true;
            }
        }
        return false;
    }

    public boolean isComparisonOperator() {
        return this.is(Token.BINARY_COMPARISON_OPERATOR, Token.ARITHMETIC_EQUALITY_BINARY_OPERATOR, Token.ARITHMETIC_INEQUALITY_BINARY_OPERATOR);
    }
    public boolean isLogicalAndArithmeticOperator() {
        return this.is(Token.COMMA, Token.SEMICOLON, Token.ARITHMETIC_UNIFY_BINARY_OPERATOR, Token.ARITHMETIC_OPERATOR);
    }

    @Override
    public FreeVars freevars() {
        return this.is(Token.VARIABLE) ? FreeVars.of(this) : TokenValue.EMPTY_LIST;
    }

    @Override
    public Term map(Subst s) {
        return s.lookup(this.toValueString()).map(value -> value.map(s)).orElse(this);
    }

    @Override
    public Optional<Subst> pmatch(Term term, Subst s) {
        return this.asVar().flatMap(var -> var.pmatch(term, s));
    }

    @Override
    public Optional<Subst> unify(Term y, Subst s) {
        return this.asVar().flatMap(var -> var.unify(y, s));
    }

    @Override
    public Optional<Constr> asConstr() {
        return this.is(Token.VARIABLE) ?
                Optional.empty()
                :
                Optional.of(new Constr(this));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TokenValue that)) return false;
        return token == that.token && Objects.equals(string, that.string);
    }

    @Override
    public int hashCode() {
        return Objects.hash(token, string);
    }

    public String predicateIndicator() {
        return this.toValueString()+"/0";
    }

    @Override
    public Optional<Var> asVar() {
        return this.is(Token.VARIABLE) ?
                Optional.of(new Var(this))
                :
                Optional.empty();
    }

    @Override
    public Term asTerm() {
        var asVar = this.asVar();
        if (asVar.isPresent()) return asVar.get();
        return asConstr().get();
    }

    public Binding asBinding() {
        return new Binding(this,this);
    }

    @Override
    public StringBuilder append(StringBuilder builder) {
        builder.append("[").append(this.token).append("=").append(this.toValueString()).append("]");
        return builder;
    }

    @Override
    public boolean isGround() {
        return this.isNotAnyOf(Token.VARIABLE, Token.ANONYMOUS_VARIABLE);
    }

    @Override
    public boolean isPartiallyInstantiated() {
        return false;
    }

    @Override
    public boolean isInstantiated() {
        return this.isNotAnyOf(Token.VARIABLE);
    }

    // chatGPT: In this context, the anonymous variable _ is used intentionally to indicate that the head of the
    // list is irrelevant for the logic of the rule. The rule is still considered instantiated in the
    // sense that it is a complete and well-defined rule.
    // only variables are used to indicate uninstantiated
    @Override
    public boolean isUnInstantiated() {
        return this.is(Token.VARIABLE);
    }
}
