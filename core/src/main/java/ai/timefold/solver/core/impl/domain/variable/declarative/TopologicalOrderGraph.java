package ai.timefold.solver.core.impl.domain.variable.declarative;

import java.util.List;
import java.util.PrimitiveIterator;

public interface TopologicalOrderGraph {
    default void startBatchChange() {
    }

    default void endBatchChange() {
    }

    default <Solution_> void withNodeData(List<EntityVariableOrFactReference<Solution_>> nodes) {
    }

    void addEdge(int from, int to);

    void removeEdge(int from, int to);

    PrimitiveIterator.OfInt nodeForwardEdges(int from);

    boolean isLooped(LoopedTracker loopedTracker, int node);

    int getTopologicalOrder(int node);
}