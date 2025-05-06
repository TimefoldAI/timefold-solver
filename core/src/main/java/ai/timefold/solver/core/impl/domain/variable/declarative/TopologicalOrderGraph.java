package ai.timefold.solver.core.impl.domain.variable.declarative;

import java.util.List;

public interface TopologicalOrderGraph extends BaseTopologicalOrderGraph {

    /**
     * Called on the first edge modification of a batch.
     */
    default void startBatchChange() {
    }

    /**
     * Called when all edge modifications are done.
     * There is no prior {@link #startBatchChange()} call if
     * no modifications were done.
     * After this method returns, {@link #getTopologicalOrder(int)}
     * must be accurate for every node in the graph.
     */
    default void endBatchChange() {
    }

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
     * <p>
     * {@link #getTopologicalOrder(int)} is allowed to be invalid
     * when this method returns.
     */
    void addEdge(int from, int to);

    /**
     * Called when a graph edge is removed.
     * <p>
     * {@link #getTopologicalOrder(int)} is allowed to be invalid
     * when this method returns.
     */
    void removeEdge(int from, int to);

}