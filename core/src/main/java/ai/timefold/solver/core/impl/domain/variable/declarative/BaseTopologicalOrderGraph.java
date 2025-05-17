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
     * Returns a tuple containing node ID and a number corresponding to its topological order.
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
    NodeTopologicalOrder getTopologicalOrder(int node);

    /**
     * If {@code order == 0},
     * then the node should be processed first.
     * Likewise, if {@code order == 1},
     * the node "y" node should be processed second.
     */
    record NodeTopologicalOrder(int nodeId, int order)
            implements
                Comparable<NodeTopologicalOrder> {

        @Override
        public int compareTo(NodeTopologicalOrder other) {
            return order - other.order;
        }

        @Override
        public boolean equals(Object o) {
            if (o instanceof NodeTopologicalOrder other) {
                return nodeId == other.nodeId;
            }
            return false;
        }

        @Override
        public int hashCode() {
            return nodeId;
        }

    }

}