package prolog.interpreter;

import prolog.nodes.CompoundNode;
import prolog.nodes.FactNode;
import prolog.nodes.PredicateNode;
import prolog.nodes.RuleNode;

import java.util.Map;
import java.util.Set;

public class Context implements AutoCloseable {
    public final Context parent;
    public final PrologRuntime runtime;
    public Subst substitions;
    public final Memory memory;

    public Context(PrologRuntime runtime) {
        this(null, runtime);
    }

    public Context(Context parent, PrologRuntime runtime) {
        this.parent = parent;
        this.runtime = runtime;
        this.memory = new Memory(this.runtime);
        this.substitions = new Subst();
    }


    @Override
    public void close() throws Exception {
        this.runtime.stop();
    }

    public Context top() {
        return this.parent == null ? this : this.parent.top();
    }

    public Map<String, Set<FactNode>> facts() {
        return this.memory.facts;
    }
    public Map<String, Set<RuleNode>> rules() {
        return this.memory.rules;
    }
    public Map<String, Set<CompoundNode>> terms() {
        return this.memory.compoundTerms;
    }


    public Set<FactNode> factsBy(String predicateIndicator) {
        return this.facts().get(predicateIndicator);
    }
    public Set<RuleNode> rulesBy(String predicateIndicator) {
        return this.rules().get(predicateIndicator);
    }


    public Set<CompoundNode> termsBy(String predicateIndicator) {
        return this.terms().get(predicateIndicator);
    }

    public void addSolution(Binding binding) {
        this.substitions = new Subst(binding, substitions);
    }
}
