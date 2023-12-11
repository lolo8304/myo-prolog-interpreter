package prolog.interpreter;

import prolog.Prolog;
import prolog.TokenValue;
import prolog.nodes.*;

import java.io.IOException;
import java.util.*;
import java.util.stream.Stream;

public class Memory {

    private final PrologRuntime runtime;

    public final Map<String, TokenValue> atoms;
    public final Map<String, TokenValue> variables;
    public final Map<String, Set<CompoundNode>> compoundTerms;
    public final Map<String, Set<FactNode>> facts;
    public final Map<String, Set<RuleNode>> rules;

    public Memory(PrologRuntime runtime) {
        this.runtime = runtime;
        this.atoms = new LinkedHashMap<>();
        this.variables = new LinkedHashMap<>();
        this.compoundTerms = new LinkedHashMap<>();
        this.facts = new LinkedHashMap<>();
        this.rules = new LinkedHashMap<>();
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
        var set = this.compoundTerms.computeIfAbsent(functorKey, k -> new LinkedHashSet<>());
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
        var set = this.rules.computeIfAbsent(predicateKey, k -> new LinkedHashSet<>());
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
        var set = this.facts.computeIfAbsent(predicateKey, k -> new LinkedHashSet<>());
        if (set.contains(fact)) {
            throw new IOException("Fact "+fact.key()+" cannot be stored twice");
        }
        set.add(fact);
        if (Prolog.verbose()) {
            System.out.println("Memory: add fact = "+fact.key());
        }
        return this;
    }

    public Stream<Map.Entry<String, Set<FactNode>>> facts() {
        return this.facts.entrySet().stream();
    }

    public Stream<Terms> clauses() {
        Stream<Terms> fs = this.facts.values().stream().flatMap(Collection::stream).map(AbstractNode::asTerms);
        Stream<Terms> rs = this.rules.values().stream().flatMap(Collection::stream).map(AbstractNode::asTerms);
        return Stream.concat(fs, rs);
    }

    public Stream<Map.Entry<String, Set<RuleNode>>> rules() {
        return this.rules.entrySet().stream();
    }

}
