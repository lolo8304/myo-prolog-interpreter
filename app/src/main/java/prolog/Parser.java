Package prolog;

public class Parser {

    public Parser(Lexer lexer) { 
        this.lexer = lexer;
    }

    // (* A Prolog program is a series of clauses *)
    //program = { clause, "." } ;
    public ProgramNode parse() {
        return parseProgram();
    }

    public ProgramNode parseProgram() {
        var node = new ProgamNode();
        var clause = this parseClause();
        while (node.isPresent()) {
            var dot = this.lexer.next();
            if (dot.token != Token.DOT) {
                throw new IOException("missing . at end of clause. received "+dot+;);
            }
            clause = this.parseClause();
        }
    }

    //* A clause can be a fact, a rule, or a query *)
    //clause = fact | rule | query ;
}