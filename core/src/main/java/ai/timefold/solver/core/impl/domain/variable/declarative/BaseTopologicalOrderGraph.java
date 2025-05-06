package ai.timefold.solver.core.impl.domain.variable.declarative;

import java.util.PrimitiveIterator;

/**
 * Exists to expose read-only view of {@link TopologicalOrderGraph}.
 */
public interface BaseTopologicalOrderGraph {

    /**
     * Return an iterator of the nodes that have the `from` node as a predecessor.
     * 
     * @param from The predecessor node.
     * @return an iterator of nodes with from as a predecessor.
     */
    PrimitiveIterator.OfInt nodeForwardEdges(int from);

    /**
     * Returns true if a given node is in a strongly connected component with a size
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
     * In particular, after {@link TopologicalOrderGraph#endBatchChange()} is called, the following
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