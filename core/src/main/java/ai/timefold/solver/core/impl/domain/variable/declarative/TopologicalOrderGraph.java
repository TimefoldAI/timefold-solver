package ai.timefold.solver.core.impl.domain.variable.declarative;

import java.util.List;
import java.util.PrimitiveIterator;

public interface TopologicalOrderGraph {
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
    default void withNodeData(List<EntityVariablePair> nodes) {
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

    /**
     * Return an iterator of the nodes that have the `from` node as a predecessor.
     * 
     * @param from The predecessor node.
     * @return an iterator of nodes with from as a predecessor.
     */
    PrimitiveIterator.OfInt nodeForwardEdges(int from);

    /**
     * Returns true is a given node is in a strongly connected component with a size
     * greater than 1 (i.e. is in a loop) or is a transitive successor of a
     * node with the above property.
     *
     * @param loopedTracker a tracker that can be used to record looped state to avoid
     *        recomputation.
     * @param node The node being queried
     * @return true if `node` is in a loop, false otherwise.
     */
    boolean isLooped(LoopedTracker loopedTracker, int node);

    /**
     * Returns a number corresponding to the topological order of a node.
     * In particular, after {@link #endBatchChange()} is called, the following
     * must be true for any pair of nodes A, B where:
     * <ul>
     * <li>A is a predecessor of B</li>
     * <li>`isLooped(A) == isLooped(B) == false`</li>
     * </ul>
     * getTopologicalOrder(A) &lt; getTopologicalOrder(B)
     * <p>
     * Said number may not be unique.
     */
    int getTopologicalOrder(int node);
}