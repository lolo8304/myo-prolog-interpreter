package prolog.interpreter;

import prolog.TokenValue;

public class Binding {

    public final String name;
    public final Term term;

    public Binding(String name, Term term) {
        this.name = name;
        this.term = term;
    }
    public Binding(TokenValue tokenValue, Term term) {
        this.name = tokenValue.toValueString();
        this.term = term;
    }
}
