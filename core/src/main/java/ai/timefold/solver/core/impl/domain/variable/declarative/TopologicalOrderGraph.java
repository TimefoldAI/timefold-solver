package ai.timefold.solver.core.impl.domain.variable.declarative;

import java.util.BitSet;
import java.util.List;

public interface TopologicalOrderGraph extends BaseTopologicalOrderGraph {

    /**
     * Called when all edge modifications are queued.
     * After this method returns, {@link #getTopologicalOrder(int)}
     * must be accurate for every node in the graph.
     */
    void commitChanges(BitSet changed);

    /**
     * Called on graph creation to supply metadata about the graph nodes.
     * 
     * @param nodes A list of entity/variable pairs, where the nth item in the list
     *        corresponds to the node with id n in the graph.
     */
    default <Solution_> void withNodeData(List<EntityVariablePair<Solution_>> nodes) {
    }

    /**
     * Called when a graph edge is added.
     * The operation is added to a batch and only executed when {@link #commitChanges(BitSet)} is called.
     * <p>
     * {@link #getTopologicalOrder(int)} is allowed to be invalid
     * when this method returns.
     */
    void addEdge(int from, int to);

    /**
     * Called when a graph edge is removed.
     * The operation is added to a batch and only executed when {@link #commitChanges(BitSet)} is called.
     * <p>
     * {@link #getTopologicalOrder(int)} is allowed to be invalid
     * when this method returns.
     */
    void removeEdge(int from, int to);

    void forEachEdge(EdgeConsumer edgeConsumer);

    @FunctionalInterface
    interface EdgeConsumer {

        void accept(int from, int to);

    }

}