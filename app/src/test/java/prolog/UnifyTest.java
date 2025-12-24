package prolog;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import prolog.interpreter.Binding;
import prolog.interpreter.Subst;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class UnifyTest extends Tester {
    @AfterEach
    protected void CloseReader() throws IOException {
        super.CloseReader();
    }



    @Test
    public void unify_2atoms() throws IOException {
        // Arrange
        var atom1 = new TokenValue(Token.ATOM, "atom1");
        var atom1_1 = new TokenValue(Token.ATOM, "atom1");
        var atom2 = new TokenValue(Token.ATOM, "atom2");

        // Action 1
        var unify1_1 = atom1.unify(atom1_1, new Subst());
        var subst1_1 = unify1_1.orElseThrow();

        // Assert
        assertEquals(0, subst1_1.size());



        // Action 2
        var unify1_2 = atom1.unify(atom2, new Subst());

        // Assert
        assertTrue(unify1_2.isEmpty());

    }


    @Test
    public void unify_atom_var() throws IOException {
        // Arrange
        var var1 = new TokenValue(Token.VARIABLE, "X");
        var atom1 = new TokenValue(Token.ATOM, "atom1");

        // Action 1
        var unifyVarAtom = var1.unify(atom1, new Subst());
        var substVarAtom = unifyVarAtom.orElseThrow();

        // Action 2
        var unifyAtomVar = atom1.unify(var1, new Subst());
        var substAtomVar = unifyAtomVar.orElseThrow();

        // Assert 1
        assertEquals(1, substVarAtom.size());
        assertEquals("X", substVarAtom.get(0).name);

        // Assert 2
        assertEquals(1, substVarAtom.size());
        assertEquals("X", substVarAtom.get(0).name);
        assertEquals("atom1", substVarAtom.get(0).term.toString());

    }



    @Test
    public void unify_2vars() throws IOException {
        // Arrange
        var var1 = new TokenValue(Token.VARIABLE, "X1");
        var var1_1 = new TokenValue(Token.VARIABLE, "X1");
        var var2 = new TokenValue(Token.VARIABLE, "Y");

        // Action 1
        var unify1_1 = var1.unify(var1_1, new Subst());
        var subst1_1 = unify1_1.orElseThrow();

        // Assert
        assertEquals(0, subst1_1.size());

        // Action 2
        var unify1_2 = var1.unify(var2, new Subst());

        // Assert
        assertTrue(unify1_2.isEmpty());

    }


    @Test
    public void unify_1varAndTerm() throws IOException {
        // Arrange
        var var1 = new TokenValue(Token.VARIABLE, "X");
        var program = this.parse("father(toni, lolo).");
        var clause = program.clauses.get(0).asConstr().orElseThrow();

        // Action 1
        var unify1 = var1.unify(clause, new Subst());
        var subst1 = unify1.orElseThrow();

        // Assert
        assertEquals(1, subst1.size());
        assertEquals("X", subst1.get(0).name);
        assertEquals("father(toni, lolo)", subst1.get(0).term.toString());


        // Action 2
        var unifyC1 = clause.unify(var1, new Subst());
        var substC1 = unifyC1.orElseThrow();

        // Assert
        assertEquals(1, substC1.size());
        assertEquals("X", substC1.get(0).name);
        assertEquals("father(toni, lolo)", substC1.get(0).term.toString());

    }



    @Test
    public void unify_1varInSubstAndTerm() throws IOException {
        // Arrange
        var var1 = new TokenValue(Token.VARIABLE, "X");
        var program = this.parse(
                "father(toni, lolo)." +
                "father(lolo, yannick).");
        var clause1 = program.clauses.get(0).asConstr().orElseThrow();
        var clause2 = program.clauses.get(1).asConstr().orElseThrow();
        var subst = new Subst(new Binding("X", clause1));

        // Action 1:
        var unify1 = var1.unify(clause2, subst);
        var subst1 = unify1;

        // Assert
        assertEquals(true, subst1.isEmpty());

    }


    @Test
    public void unify_2Lists() throws IOException {
        // Arrange
        var program = this.parse(
                "[ 1, 2, 3 ]." +
                        "[ 1, 2, 3 ]." +
                        "[ 10, 20, 30 ].");
        var list1 = program.clauses.get(0).asConstr().orElseThrow();
        var list11 = program.clauses.get(1).asConstr().orElseThrow();
        var list2 = program.clauses.get(2).asConstr().orElseThrow();
        var subst = new Subst();

        // Action 1:
        var unify1 = list1.unify(list11, subst);
        var subst1 = unify1.orElseThrow();

        // Assert
        assertEquals(0, subst1.size());


        // Action 2:
        var unify12 = list1.unify(list2, subst);
        var subst12 = unify12;

        // Assert
        assertEquals(true, subst12.isEmpty());

    }


    @Test
    public void unify_2lists_empty() throws IOException {
        // Arrange
        var program = this.parse(
                "[  ]." +
                        "[ ]." +
                        "[ 10, 20, 30 ].");
        var list1 = program.clauses.get(0).asConstr().orElseThrow();
        var list11 = program.clauses.get(1).asConstr().orElseThrow();
        var list2 = program.clauses.get(2).asConstr().orElseThrow();
        var subst = new Subst();

        // Action 1:
        var unify1 = list1.unify(list11, subst);
        var subst1 = unify1.orElseThrow();

        // Assert
        assertEquals(0, subst1.size());


        // Action 2:
        var unify12 = list1.unify(list2, subst);
        var subst12 = unify12;

        // Assert
        assertEquals(true, subst12.isEmpty());

    }




    @Test
    public void unify_2Lists_with_vars() throws IOException {
        // Arrange
        var program = this.parse(
                "[ a, X, c ]." +
                        "[ a, b, c ]." +
                        "[ aaa, b, c ].");
        var list1 = program.clauses.get(0).asConstr().orElseThrow();
        var list11 = program.clauses.get(1).asConstr().orElseThrow();
        var list2 = program.clauses.get(2).asConstr().orElseThrow();
        var subst = new Subst();

        // Action 1:
        var unify1 = list1.unify(list11, subst);
        var subst1 = unify1.orElseThrow();

        // Assert
        assertEquals(1, subst1.size());
        assertEquals("X", subst1.get(0).name);
        assertEquals("b", subst1.get(0).term.toString());


        // Action 2:
        var subst2 = new Subst();
        var unify12 = list1.unify(list2, subst2);
        var subst12 = unify12;

        // Assert
        assertEquals(true, subst12.isEmpty());

    }


    @Test
    public void unify_head_and_tail() throws IOException {
        // Arrange
        var program = this.parse(
                "[ a | [ X ] ]." +
                        "[ a, b ]." +
                        "[ a | [ b ] ].");
        var list1 = program.clauses.get(0).asConstr().orElseThrow();
        var list11 = program.clauses.get(1).asConstr().orElseThrow();
        var list12 = program.clauses.get(2).asConstr().orElseThrow();
        var subst1 = new Subst();
        var subst2 = new Subst();

        // Action 1:
        var unify11 = list1.unify(list11, subst1);
        var subst11 = unify11.orElseThrow();

        var unify12 = list1.unify(list12, subst2);
        var subst12 = unify12.orElseThrow();

        // Assert
        assertEquals(1, subst11.size());
        assertEquals("X", subst11.get(0).name);
        assertEquals("b", subst11.get(0).term.toString());

        // Assert
        assertEquals(1, subst12.size());
        assertEquals("X", subst12.get(0).name);
        assertEquals("b", subst12.get(0).term.toString());

    }


    @Test
    public void unify_head_and_tailAsVar() throws IOException {
        // Arrange
        var program = this.parse(
                "f([ a | X ])." +
                        "f([ a, b ])." +
                        "f([ a | [ b | [] ] ]).");
        var list1 = program.clauses.get(0).asConstr().orElseThrow();
        var list11 = program.clauses.get(1).asConstr().orElseThrow();
        var list12 = program.clauses.get(2).asConstr().orElseThrow();
        var subst1 = new Subst();
        var subst2 = new Subst();

        // Action 1:
        var unify11 = list1.unify(list11, subst1);
        var subst11 = unify11.orElseThrow();

        var unify12 = list1.unify(list12, subst2);
        var subst12 = unify12.orElseThrow();

        // Assert
        assertEquals(1, subst11.size());
        assertEquals("X", subst11.get(0).name);
        assertEquals("[b | [] ]", subst11.get(0).term.toString());

        // Assert
        assertEquals(1, subst12.size());
        assertEquals("X", subst12.get(0).name);
        assertEquals("[b | [] ]", subst12.get(0).term.toString());

    }


    @Test
    public void unify_headVar_and_tail() throws IOException {
        // Arrange
        var program = this.parse(
                "f([ a | [ b ] ])." +
                        "f([ X, b ])." +
                        "f([ X | [ b | [] ] ]).");
        var list1 = program.clauses.get(0).asConstr().orElseThrow();
        var list11 = program.clauses.get(1).asConstr().orElseThrow();
        var list12 = program.clauses.get(2).asConstr().orElseThrow();
        var subst1 = new Subst();
        var subst2 = new Subst();

        // Action 1:
        var unify11 = list1.unify(list11, subst1);
        var subst11 = unify11.orElseThrow();

        var unify12 = list1.unify(list12, subst2);
        var subst12 = unify12.orElseThrow();

        // Assert
        assertEquals(1, subst11.size());
        assertEquals("X", subst11.get(0).name);
        assertEquals("a", subst11.get(0).term.toString());

        // Assert
        assertEquals(1, subst12.size());
        assertEquals("X", subst12.get(0).name);
        assertEquals("a", subst12.get(0).term.toString());

    }


}
