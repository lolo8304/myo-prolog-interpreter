package prolog;

import prolog.nodes.*;

import java.io.IOException;
import java.util.*;

import static prolog.nodes.ArgumentNode.NIL_ARGUMENT;

public class Parser {

    private final Lexer lexer;
    private Deque<TokenValue> queue;
    private List<TokenValue> comments;

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
        var fact = this.parseFact();
        if (fact.isEmpty()) {
            return Optional.empty();
        }
        var unify = this.readNextToken();
        if (unify.token == Token.UNIFY) {
            var body = this.parseExpression();
            return Optional.of(new ClauseNode(new RuleNode(fact.get().predicate, body)));
        } else {
            this.pushBackToken(unify);
            return Optional.of(new ClauseNode(fact.get()));
        }
    }

    public Optional<FactNode> parseFact() throws IOException {
        return this.parsePredicate().map(FactNode::new);
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

    private ExpressionNode parseExpression() throws IOException {
        var argument = this.parseArgument();
        var comparison_operator = this.readNextToken();
        if (comparison_operator.isComparisonOperator()) {
            var otherArgument = this.parseArgument();
            return new ExpressionNode(new ConditionNode(argument, comparison_operator, otherArgument));
        } else if (comparison_operator.isLogicalAndArithmeticOperator()) {
            var otherExpression = this.parseExpression();
            return new ExpressionNode(new LogicalExpressionNode(new ExpressionNode(argument), comparison_operator, otherExpression));
        } else {
            this.pushBackToken(comparison_operator);
            return new ExpressionNode(argument);
        }
    }

    //(* A predicate is an atom, possibly followed by a list of arguments *)
    //predicate = atom, [ "(", argument, { ",", argument }, ")" ] ;
    private Optional<PredicateNode> parsePredicate() throws IOException {
        var atom = this.readNextToken();
        if (atom.token == Token.EOF) {
            return Optional.empty();
        }
        if (atom.isNotAnyOf(Token.ATOM, Token.QUOTED_ATOM)) {
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


    // (* Arguments can be atoms, variables, numbers, or compound terms *)
    //argument = atom | variable | number | predicate ;
    private ArgumentNode parseArgument() throws IOException {
        var argument = this.readNextToken();
        if (argument.is(Token.VARIABLE, Token.NUMBER, Token.ANONYMOUS_VARIABLE)) {
            return new ArgumentNode(argument);
        } else if (argument.is(Token.ATOM, Token.QUOTED_ATOM)) {
            var lparent = this.readNextToken();
            if (lparent.is(Token.OPEN_PARENTHESIS)) {
                // compound - pushback 2
                this.pushBackToken(lparent);
                this.pushBackToken(argument);
                return new ArgumentNode(this.parseCompoundTerm());
            } else {
                this.pushBackToken(lparent);
            }
            return new ArgumentNode(argument);
        } else if (argument.is(Token.nil)) {
            return NIL_ARGUMENT;
        } else if (argument.is(Token.ARRAY_OF_CHARACTERS)) {
            this.pushBackToken(argument);
            return new ArgumentNode(this.parseListNotationFromString());
        } else if (argument.is(Token.OPEN_LIST)) {
            this.pushBackToken(argument);
            var compound = this.parseListNotation();
            return new ArgumentNode(compound);
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
                for (int i = list.size()-1; i >= 0; i--) {
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