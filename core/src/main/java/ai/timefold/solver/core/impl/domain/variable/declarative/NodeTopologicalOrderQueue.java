package ai.timefold.solver.core.impl.domain.variable.declarative;

import java.util.BitSet;
import java.util.NoSuchElementException;

import org.jspecify.annotations.NullMarked;

/**
 * A binary min-heap of graph node ids, ordered by {@link BaseTopologicalOrderGraph#getTopologicalOrder(int)}.
 * It replaces a {@code PriorityQueue<NodeTopologicalOrder>} on the declarative shadow variable update path,
 * where profiling showed the heap to be a fifth of the solver's CPU time.
 * It differs from that queue in two ways, both of which leave the processing order intact.
 * <p>
 * <b>The topological order is read once, when a node enters the queue, and is then held in {@link #orders}.</b>
 * {@code NodeTopologicalOrder.compareTo} instead reads it through the {@link BaseTopologicalOrderGraph} interface
 * twice per comparison, which costs an interface dispatch and, in some implementations, more than an array read -
 * and one upheap or downheap makes {@code O(log n)} comparisons.
 * Caching is sound because the order of a node cannot change while nodes sit in the queue:
 * {@link TopologicalOrderGraph#commitChanges(BitSet)} has already run by then, and nothing the consumer of this
 * queue does between {@link #offer} and {@link #poll} touches the graph's edges.
 * <p>
 * <b>A node already in the queue is not queued again.</b>
 * The caller of the {@code PriorityQueue} could enqueue one node once per incoming edge and drop the duplicates
 * when they came back out, so the heap held - and re-ordered - entries it would go on to discard.
 * Skipping them on the way in changes nothing about which nodes are processed, or in which order,
 * and bounds the queue at one entry per node, so the backing arrays never have to grow.
 * <p>
 * Ties are broken arbitrarily, as they are by a {@link java.util.PriorityQueue}.
 * Two nodes that are not looped share a topological order only when neither is a predecessor of the other,
 * so neither one's update can feed the other's. A looped node can tie with its own predecessor, because
 * {@link BaseTopologicalOrderGraph#getTopologicalOrder(int)} orders a predecessor pair only when neither
 * node is looped. That is safe here: the consumer gives a looped node a null value instead of one computed
 * from its predecessors, so the order in which looped nodes come out does not affect the outcome either.
 * <p>
 * This class is not thread safe.
 */
@NullMarked
final class NodeTopologicalOrderQueue {

    private final BaseTopologicalOrderGraph graph;
    /**
     * The heap itself, holding node ids in {@code [0, size)}.
     * A node occupies at most one slot, so the graph's node count is also the capacity.
     */
    private final int[] nodeIds;
    /**
     * The topological order of the node in the matching slot of {@link #nodeIds},
     * as it was when that node entered the queue. {@link #upheap} and {@link #downheap} compare these ints
     * and never call back into the graph.
     */
    private final int[] orders;
    /**
     * The nodes currently in the queue, so that a second {@link #offer} of one of them can be dropped.
     * A bit is set by {@link #offer} and cleared by {@link #poll}, which leaves the set empty once the queue drains;
     * no bulk clear is needed between uses.
     */
    private final BitSet queued;
    private int size;

    NodeTopologicalOrderQueue(BaseTopologicalOrderGraph graph, int nodeCount) {
        this.graph = graph;
        this.nodeIds = new int[nodeCount];
        this.orders = new int[nodeCount];
        this.queued = new BitSet(nodeCount);
        this.size = 0;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    /**
     * Adds the node to the queue, unless it is already in it.
     *
     * @param nodeId must be {@code >= 0} and {@code < nodeCount}
     */
    public void offer(int nodeId) {
        if (queued.get(nodeId)) {
            return;
        }
        queued.set(nodeId);
        var index = size;
        size++;
        nodeIds[index] = nodeId;
        orders[index] = graph.getTopologicalOrder(nodeId);
        upheap(index);
    }

    /**
     * Removes and returns the node with the lowest topological order.
     *
     * @throws NoSuchElementException if the queue is empty
     */
    public int poll() {
        if (size == 0) {
            throw new NoSuchElementException("The queue is empty.");
        }
        var polledNodeId = nodeIds[0];
        queued.clear(polledNodeId);
        size--;
        var lastIndex = size;
        if (lastIndex > 0) {
            nodeIds[0] = nodeIds[lastIndex];
            orders[0] = orders[lastIndex];
            downheap(0);
        }
        return polledNodeId;
    }

    /**
     * Moves the node at {@code startIndex} towards the root, until its parent comes before it.
     * This is {@code java.util.PriorityQueue.siftUpComparable}, specialised to {@code int}.
     * Note that other libraries give the name the opposite sense: CPython's {@code heapq._siftdown}
     * is this method, because Floyd named the routine after the elements that move the other way.
     */
    private void upheap(int startIndex) {
        // The node does not move one slot at a time. Parents move down into the hole at index,
        // and the node goes into the last hole when the loop ends.
        var index = startIndex;
        var nodeId = nodeIds[index];
        var order = orders[index];
        while (index > 0) {
            var parentIndex = (index - 1) >>> 1;
            if (orders[parentIndex] <= order) {
                break;
            }
            // The parent comes later than the node, so move the parent down into the hole.
            nodeIds[index] = nodeIds[parentIndex];
            orders[index] = orders[parentIndex];
            index = parentIndex;
        }
        nodeIds[index] = nodeId;
        orders[index] = order;
    }

    /**
     * Moves the node at {@code startIndex} towards the leaves, until it comes before both of its children.
     * This is {@code java.util.PriorityQueue.siftDownComparable}, specialised to {@code int}.
     * See {@link #upheap} on the name.
     */
    private void downheap(int startIndex) {
        // Children move up into the hole at index, and the node goes into the last hole when the loop ends.
        var index = startIndex;
        var nodeId = nodeIds[index];
        var order = orders[index];
        var half = size >>> 1; // A node at or past the halfway point has no child.
        while (index < half) {
            var childIndex = (index << 1) + 1;
            var rightIndex = childIndex + 1;
            if (rightIndex < size && orders[rightIndex] < orders[childIndex]) {
                childIndex = rightIndex; // Of the two children, keep the one that comes first.
            }
            if (order <= orders[childIndex]) {
                break;
            }
            // The node comes later than that child, so move the child up into the hole.
            nodeIds[index] = nodeIds[childIndex];
            orders[index] = orders[childIndex];
            index = childIndex;
        }
        nodeIds[index] = nodeId;
        orders[index] = order;
    }

}
