package prolog;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.StringReader;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class PrologCliTest {

    @Test
    public void user_consult_adds_entered_facts_and_rules() throws IOException {
        var cli = new PrologCli(false);
        cli.executeUserConsult(new StringReader(
                "father(toni,lolo)." +
                "likes(X,pizza)." +
                "parent(X,Y) :- father(X,Y)."));

        var output = new ByteArrayOutputStream();
        var previousOut = System.out;
        try {
            System.setOut(new PrintStream(output));
            cli.execute(new StringReader("parent(toni, Who)."));
        } finally {
            System.setOut(previousOut);
        }

        assertTrue(output.toString().contains("Who = lolo"), output.toString());

        output.reset();
        try {
            System.setOut(new PrintStream(output));
            cli.execute(new StringReader("likes(alice, pizza)."));
        } finally {
            System.setOut(previousOut);
        }

        assertTrue(output.toString().contains("true."), output.toString());
    }

    @Test
    public void user_reconsult_overwrites_existing_facts_and_rules_by_predicate() throws IOException {
        var cli = new PrologCli(false);
        cli.executeUserConsult(new StringReader(
                "parent(toni,lolo)." +
                "parent(lolo,yannick)." +
                "ancestor(X,Y) :- parent(X,Y)."));

        cli.executeUserConsult(new StringReader(
                "parent(alice,bob)." +
                "parent(bob,carol)." +
                "ancestor(X,Y) :- parent(X,Z), parent(Z,Y)."), true);

        var output = new ByteArrayOutputStream();
        var previousOut = System.out;
        try {
            System.setOut(new PrintStream(output));
            cli.execute(new StringReader("parent(toni, lolo)."));
            cli.execute(new StringReader("parent(alice, bob)."));
            cli.execute(new StringReader("ancestor(alice, Who)."));
        } finally {
            System.setOut(previousOut);
        }

        assertTrue(output.toString().contains("false."), output.toString());
        assertTrue(output.toString().contains("true."), output.toString());
        assertTrue(output.toString().contains("Who = carol"), output.toString());
    }

    @Test
    public void user_reconsult_allows_replacing_the_same_fact() throws IOException {
        var cli = new PrologCli(false);
        cli.executeUserConsult(new StringReader("color(blue)."));
        cli.executeUserConsult(new StringReader("color(blue)."), true);

        var output = new ByteArrayOutputStream();
        var previousOut = System.out;
        try {
            System.setOut(new PrintStream(output));
            cli.execute(new StringReader("color(blue)."));
        } finally {
            System.setOut(previousOut);
        }

        assertTrue(output.toString().contains("true."), output.toString());
    }
}
