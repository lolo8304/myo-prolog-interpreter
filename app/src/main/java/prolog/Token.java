package prolog;

public enum Token {
    POINT,
    UNIFY (false, true),
    AND_OPERATOR (false, true ),
    OPEN_PARENTHESIS,
    CLOSE_PARENTHESIS,
    ATOM(true),
    NUMBER (true),
    VARIABLE (true),
    EOF,
    QUOTED_ATOM (true),
    COMMENT,
    ARITHMETIC_OPERATOR (false, true),
    CLOSE_ARRAY,
    OPEN_ARRAY,
    BINARY_COMPARISON_OPERATOR (false, true),
    ARITHMETIC_INEQUALITY_BINARY_OPERATOR (false, true),
    ARRAY_OF_CHARACTERS (true),
    QUERY,
    UNIFY_WITH_OCCUR_CHECK (false, true),
    CUT (false, true),
    OR_OPERATOR (false, true),
    DECOMPOSITION_OPERATOR (false, true),
    ATOM_WILDCARD (true),
    UNARY_NOT_OPERATOR (false, true, true),
    ARITHMETIC_EQUALITY_BINARY_OPERATOR (false, true);

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
