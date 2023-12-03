package prolog.nodes;

public abstract class AbstractNode implements Node {

    @Override
    public String toString() {
        return this.append(new StringBuilder()).toString();
    }
}
