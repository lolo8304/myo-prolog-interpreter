package prolog;

public enum Token {
    POINT,
    OPEN_PARENTHESIS,
    CLOSE_PARENTHESIS,
    ATOM(true),
    nil(true),
    NUMBER (true),
    VARIABLE (true),
    EOF,
    QUOTED_ATOM (true),
    COMMENT,
    CLOSE_LIST,
    OPEN_LIST,

    ARITHMETIC_OPERATOR (false, true),
    UNIFY (false, true),

    COMMA(false, true ),
    SEMICOLON(false, true),

    BINARY_COMPARISON_OPERATOR (false, true),
    ARITHMETIC_INEQUALITY_BINARY_OPERATOR (false, true),
    ARITHMETIC_EQUALITY_BINARY_OPERATOR (false, true),
    ARITHMETIC_UNIFY_BINARY_OPERATOR (false, true),

    DECOMPOSITION_OPERATOR (false, true),
    CUT (false, true),

    UNARY_NOT_OPERATOR (false, true, true),

    UNIFY_WITH_OCCUR_CHECK (false),
    ARRAY_OF_CHARACTERS (true),
    QUERY,
    ANONYMOUS_VARIABLE(true);


    public final boolean isOperator;
    public final boolean isAtom;
    public final boolean isUnaryOperator;

    Token() {
        this(false, false, false);
    }
    Token(boolean isAtom) {
        this(isAtom, false, false);
    }
    Token(boolean isAtom, boolean isOperator) {
        this(isAtom, isOperator, false);
    }
    Token(boolean isAtom, boolean isOperator, boolean isUnaryOperator) {
        this.isAtom = isAtom;
        this.isOperator = isOperator;
        this.isUnaryOperator = isUnaryOperator;
    }

}
