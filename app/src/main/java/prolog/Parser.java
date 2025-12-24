package prolog;

import prolog.nodes.*;

import java.io.IOException;
import java.util.*;

import static prolog.nodes.ArgumentNode.NIL_ARGUMENT;

public class Parser {

    private final Lexer lexer;
    private final Deque<TokenValue> queue;
    private final List<TokenValue> comments;

    public Parser(Lexer lexer) { 
        this.lexer = lexer;
        this.queue = new LinkedList<>();
        this.comments = new ArrayList<>();
    }

    private void pushBackToken(TokenValue token) {
        queue.push(token);
    }

    public List<TokenValue> comments() {
        return this.comments;
    }

    private TokenValue readNextToken() throws IOException {
        if (queue.isEmpty()) {
            var next = this.lexer.next();
            while (next.is(Token.COMMENT)) {
                this.comments.add(next);
                next = this.lexer.next();
            }
            return next;
        }
        return queue.pop();
    }

    // (* A Prolog program is a series of clauses *)
    //program = { clause, "." } ;
    public Optional<ProgramNode> parse() throws IOException {
        return this.parseProgram();
    }

    public Optional<ProgramNode> parseProgram() throws IOException {
        var clause = this.parseClause();
        if (clause.isEmpty()) {
            return Optional.empty();
        }
        var node = new ProgramNode();
        while (clause.isPresent()) {
            node.addClause(clause.get());
            var dot = this.readNextToken();
            if (dot.token != Token.POINT) {
                throw new IOException("missing . at end of clause. received token '"+dot+"'");
            }
            clause = this.parseClause();
        }
        return Optional.of(node);
    }

    //* A clause can be a fact, a rule, or a query *)
    //clause = predicate | rule ;
    private Optional<ClauseNode> parseClause() throws IOException {
        var exp = this.parseExpression();
        if (exp.isEmpty()) {
            return Optional.empty();
        }
        var predicate = exp.get().asPredicate();
        if (predicate.isPresent()) {
            var fact = new FactNode(predicate.get());
            var unify = this.readNextToken();
            if (unify.is(Token.UNIFY)) {
                var body = this.parseExpression();
                if (body.isEmpty()) {
                    throw new IOException("Body expected but EOF");
                }
                return Optional.of(new ClauseNode(new RuleNode(fact.predicate, body.get())));
            } else {
                this.pushBackToken(unify);
                return Optional.of(new ClauseNode(fact));
            }
        } else {
            return Optional.of(new ClauseNode(exp.get()));
        }
    }

    // expression = term | condition | expression, logical_operator, expression ;
    //
    //(* A condition can include comparison operators *)
    //condition = term, comparison_operator, term ;
    //
    //(* Logical operators *)
    //logical_operator = "," | ";" ;
    //
    //(* Comparison operators *)
    //comparison_operator = "=" | "\\=" | "<" | ">" | "=<" | ">=" ;
    //
    // term = argument;

    private Optional<ExpressionNode> parseExpression() throws IOException {
        var argument = this.parseArgumentOptional();
        if (argument.isEmpty()) return Optional.empty();

        var operator = this.readNextToken();
        if (operator.isComparisonOperator()) {
            var listOfArguments = new ArrayList<ArgumentNode>();
            var listOfConditions = new ArrayList<TokenValue>();
            while (operator.isComparisonOperator()) {
                listOfArguments.add(argument.get());
                listOfConditions.add(operator);
                argument = this.parseArgumentOptional();
                operator = this.readNextToken();
            }
            listOfArguments.add(argument.get());
            this.pushBackToken(operator);
            return Optional.of(new ExpressionNode(new ConditionNode(listOfArguments, listOfConditions)));

        } else if (operator.isLogicalAndArithmeticOperator()) {
            var listOfArguments = new ArrayList<ArgumentNode>();
            var listOfConditions = new ArrayList<TokenValue>();
            while (operator.isLogicalAndArithmeticOperator()) {
                listOfArguments.add(argument.get());
                listOfConditions.add(operator);
                argument = this.parseArgumentOptional();
                operator = this.readNextToken();
            }
            listOfArguments.add(argument.get());
            this.pushBackToken(operator);
            return Optional.of(new ExpressionNode(new LogicalExpressionNode(listOfArguments, listOfConditions)));
        } else {
            this.pushBackToken(operator);
            return Optional.of(new ExpressionNode(argument.get()));
        }
    }

    //(* A predicate is an atom, possibly followed by a list of arguments *)
    //predicate = atom, [ "(", argument, { ",", argument }, ")" ] ;
    private Optional<PredicateNode> parsePredicate() throws IOException {
        var atom = this.readNextToken();
        if (atom.token == Token.EOF) {
            return Optional.empty();
        }
        if (atom.isNotAnyOf(Token.ATOM, Token.QUOTED_ATOM, Token.VARIABLE, Token.NUMBER)) {
            throw new IOException("Predicate must start with an atom or single quotes but was token '"+atom+"'");
        }
        var lparent = this.readNextToken();
        var predicate = new PredicateNode(atom);
        if (lparent.token == Token.OPEN_PARENTHESIS) {
            var argument = this.parseArgument();
            var comma = this.readNextToken();
            while (comma.token == Token.COMMA) {
                predicate.addArgument(argument);
                argument = this.parseArgument();
                comma = this.readNextToken();
            }
            predicate.addArgument(argument);
            if (comma.token != Token.CLOSE_PARENTHESIS) {
                throw new IOException("Predicates arguments must finished with ) but token was '"+comma+"'");
            }
        } else {
            this.pushBackToken(lparent);
        }
        return Optional.of(predicate);
    }

    //(* A predicate is an atom, possibly followed by a list of arguments *)
    //predicate = atom, [ "(", argument, { ",", argument }, ")" ] ;
    private CompoundNode parseCompoundTerm() throws IOException {
        var atom = this.readNextToken();
        if (atom.token == Token.EOF) {
            throw new IOException("Compound term needs arguments starting with ( but received EOF");
        }
        if (atom.isNotAnyOf(Token.ATOM, Token.QUOTED_ATOM)) {
            throw new IOException("Compound term must start with an atom but was token '"+atom+"'");
        }
        var lparent = this.readNextToken();
        if (lparent.token != Token.OPEN_PARENTHESIS) {
            throw new IOException("Compound term needs arguments starting with ( but received '"+lparent+"'");
        }
        var compoundTermNode = new CompoundTermNode(atom);
        var argument = this.parseArgument();
        var comma = this.readNextToken();
        while (comma.token == Token.COMMA) {
            compoundTermNode.addArgument(argument);
            argument = this.parseArgument();
            comma = this.readNextToken();
        }
        compoundTermNode.addArgument(argument);
        if (comma.token != Token.CLOSE_PARENTHESIS) {
            throw new IOException("Predicates arguments must finished with ) but token was '"+comma+"'");
        }
        return compoundTermNode.tryAsListNotation();
    }

    private ArgumentNode parseArgument() throws IOException {
        return this.parseArgumentOptional().get();
    }

    // (* Arguments can be atoms, variables, numbers, or compound terms *)
    //argument = atom | variable | number | predicate ;
    private Optional<ArgumentNode> parseArgumentOptional() throws IOException {
        var argument = this.readNextToken();
        if (argument.is(Token.EOF)) return Optional.empty();
        if (argument.is(Token.VARIABLE, Token.NUMBER, Token.ANONYMOUS_VARIABLE)) {
            return Optional.of(new ArgumentNode(argument));
        } else if (argument.is(Token.ATOM, Token.QUOTED_ATOM)) {
            var lparent = this.readNextToken();
            if (lparent.is(Token.OPEN_PARENTHESIS)) {
                // compound - pushback 2
                this.pushBackToken(lparent);
                this.pushBackToken(argument);
                return Optional.of(new ArgumentNode(this.parseCompoundTerm()));
            } else {
                this.pushBackToken(lparent);
            }
            return Optional.of(new ArgumentNode(argument));
        } else if (argument.is(Token.nil)) {
            return Optional.of(NIL_ARGUMENT);
        } else if (argument.is(Token.ARRAY_OF_CHARACTERS)) {
            this.pushBackToken(argument);
            return Optional.of(new ArgumentNode(this.parseListNotationFromString()));
        } else if (argument.is(Token.OPEN_LIST)) {
            this.pushBackToken(argument);
            var compound = this.parseListNotation();
            return Optional.of(new ArgumentNode(compound));
        } else if (argument.toValueString().equals("+") || argument.toValueString().equals("-")) {
            // check if next is a
            //      number or (....)
            var arg = this.parseArgument();
            return Optional.of(new ArgumentNode(new CompoundTermNode(argument, Collections.singletonList(arg))));
        }
        throw new IOException("Illegal token found for argument node '"+argument+"'");
    }

    private CompoundNode parseListNotationFromString() throws IOException {
        var open = this.readNextToken();
        if (open.is(Token.ARRAY_OF_CHARACTERS)) {
            var arrayToken = (TokenArrayValue)open;
            ArgumentNode lastTail = NIL_ARGUMENT;
            for (int i = arrayToken.array.length-1; i >= 0; i--) {
                var atom = new TokenValue(Token.ATOM, String.valueOf((char)arrayToken.array[i]));
                var argument = new ArgumentNode(atom);
                lastTail = new ArgumentNode(new CompoundListNode(argument, lastTail));
            }
            return lastTail.compoundTerm;
        } else {
            throw new IOException("Illegal token found instead of [ of chars ] for list notation node '"+open+"'");
        }
    }

    private CompoundNode parseListNotation() throws IOException {
        var open = this.readNextToken();
        if (open.is(Token.OPEN_LIST)) {
            var atom = new TokenValue(Token.ATOM, ".");
            var close = this.readNextToken();
            if (close.is(Token.CLOSE_LIST)) {
                return new CompoundTermNode(TokenValue.NIL);
            }
            pushBackToken(close);
            var head = this.parseArgument();
            close = this.readNextToken();
            if (close.is(Token.DECOMPOSITION_OPERATOR)) {
                var tail = this.parseArgument();
                close = this.readNextToken();
                if (!close.is(Token.CLOSE_LIST)) {
                    throw new IOException("Illegal token found instead of ] for list notation node '"+close+"'");
                }
                return new CompoundListNode(head, tail);
            } else if (close.is(Token.COMMA)) {
                var list = new ArrayList<ArgumentNode>();
                list.add(head);
                var argument = this.parseArgument();
                list.add(argument);
                close = this.readNextToken();
                while (close.is(Token.COMMA)) {
                    argument = this.parseArgument();
                    list.add(argument);
                    close = this.readNextToken();
                }
                ArgumentNode lastTail = NIL_ARGUMENT;
                if (close.is(Token.DECOMPOSITION_OPERATOR)) {
                    lastTail = this.parseArgument();
                    close = this.readNextToken();
                }
                if (!close.is(Token.CLOSE_LIST)) {
                    throw new IOException("Illegal token found instead of ] for list notation node '"+close+"'");
                }
                var startIndex = list.size()-1;
                if (list.get(startIndex) == NIL_ARGUMENT) {
                    startIndex--;
                }
                for (int i = startIndex; i >= 0; i--) {
                    lastTail = new ArgumentNode(new CompoundListNode(list.get(i), lastTail));
                }
                return lastTail.compoundTerm;
            } else if (close.is(Token.CLOSE_LIST)) {
                return new CompoundListNode(head, NIL_ARGUMENT);
            } else {
                throw new IOException("Illegal token found instead of ] for list notation node '"+close+"'");
            }
        }
        throw new IOException("Illegal token found instead of [ for list notation node '"+open+"'");
    }

}