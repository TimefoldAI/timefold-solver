package ai.timefold.solver.core.impl.domain.variable.provided;

import java.util.PrimitiveIterator;

public interface TopologicalOrderGraph {
    default void startBatchChange() {
    }

    default void endBatchChange() {
    }

    void addEdge(int from, int to);

    void removeEdge(int from, int to);

    PrimitiveIterator.OfInt componentForwardEdges(int from);

    boolean isLooped(LoopedTracker loopedTracker, int node);

    int getTopologicalOrder(int node);
}