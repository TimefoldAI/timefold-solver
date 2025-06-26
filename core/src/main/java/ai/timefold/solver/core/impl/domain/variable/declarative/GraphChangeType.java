package ai.timefold.solver.core.impl.domain.variable.declarative;

public enum GraphChangeType {
    NO_CHANGE(false),
    ADD_EDGE(true),
    REMOVE_EDGE(true);

    private final boolean affectsGraph;

    GraphChangeType(boolean affectsGraph) {
        this.affectsGraph = affectsGraph;
    }

    public boolean affectsGraph() {
        return affectsGraph;
    }
}
