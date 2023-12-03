package prolog;

import prolog.nodes.CompoundNode;
import prolog.nodes.FactNode;
import prolog.nodes.RuleNode;

import java.io.IOException;
import java.util.*;

public class Memory {

    private final PrologRuntime runtime;

    private final Map<String, TokenValue> atoms;
    private final Map<String, TokenValue> variables;
    private final Map<String, Set<CompoundNode>> compoundTerms;
    private final Map<String, Set<FactNode>> facts;
    private final Map<String, Set<RuleNode>> rules;

    public Memory(PrologRuntime runtime) {
        this.runtime = runtime;
        this.atoms = new HashMap<>();
        this.variables = new HashMap<>();
        this.compoundTerms = new HashMap<>();
        this.facts = new HashMap<>();
        this.rules = new HashMap<>();
    }

    public Memory addAtom(TokenValue atom) throws IOException {
        var key = atom.toValueString();
        var found = this.atoms.get(key);
        if (found != null) {
            throw new IOException("Atom cannot be stored twice");
        }
        if (Prolog.verbose()) {
            System.out.println("Memory: add atom = "+atom.toValueString());
        }
        this.atoms.put(key, atom);
        return this;
    }
    public Memory addVariable(TokenValue variable) throws IOException {
        var key = variable.toValueString();
        var found = this.variables.get(key);
        if (found != null) {
            throw new IOException("Variable "+key+" cannot be stored twice");
        }
        if (Prolog.verbose()) {
            System.out.println("Memory: add variable = "+variable.toValueString());
        }
        this.variables.put(key, variable);
        return this;
    }
    public Memory addCompoundTerm(CompoundNode compoundTerm) throws IOException {
        var functorKey = compoundTerm.principalFunctor();
        var set = this.compoundTerms.computeIfAbsent(functorKey, k -> new HashSet<>());
        if (set.contains(compoundTerm)) {
            throw new IOException("Compound term "+compoundTerm.key()+" cannot be stored twice");
        }
        set.add(compoundTerm);
        if (Prolog.verbose()) {
            System.out.println("Memory: add compound = "+compoundTerm.key());
        }
        return this;
    }

    public Memory addRule(RuleNode rule) throws IOException {
        var predicateKey = rule.head.predicateIndicator();
        var set = this.rules.computeIfAbsent(predicateKey, k -> new HashSet<>());
        if (set.contains(rule)) {
            throw new IOException("Rule "+rule.key()+" cannot be stored twice");
        }
        set.add(rule);
        if (Prolog.verbose()) {
            System.out.println("Memory: add rule = "+rule.key());
        }
        return this;
    }
    public Memory addFact(FactNode fact) throws IOException {
        var predicateKey = fact.predicate.predicateIndicator();
        var set = this.facts.computeIfAbsent(predicateKey, k -> new HashSet<>());
        if (set.contains(fact)) {
            throw new IOException("Fact "+fact.key()+" cannot be stored twice");
        }
        set.add(fact);
        if (Prolog.verbose()) {
            System.out.println("Memory: add fact = "+fact.key());
        }
        return this;
    }
}
