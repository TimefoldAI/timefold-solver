package ai.timefold.solver.core.impl.domain.variable.declarative;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.PrimitiveIterator;
import java.util.PriorityQueue;
import java.util.Random;

import org.junit.jupiter.api.Test;

class NodeTopologicalOrderQueueTest {

    /**
     * Only {@link BaseTopologicalOrderGraph#getTopologicalOrder(int)} matters to the queue.
     */
    private static BaseTopologicalOrderGraph graphWithOrders(int... orders) {
        return new BaseTopologicalOrderGraph() {

            @Override
            public PrimitiveIterator.OfInt nodeForwardEdges(int from) {
                throw new UnsupportedOperationException();
            }

            @Override
            public boolean isLooped(LoopedTracker loopedTracker, int node) {
                throw new UnsupportedOperationException();
            }

            @Override
            public int getTopologicalOrder(int node) {
                return orders[node];
            }
        };
    }

    private static List<Integer> drain(NodeTopologicalOrderQueue queue) {
        var out = new ArrayList<Integer>();
        while (!queue.isEmpty()) {
            out.add(queue.poll());
        }
        return out;
    }

    @Test
    void emptyQueue() {
        var queue = new NodeTopologicalOrderQueue(graphWithOrders(0, 1, 2), 3);

        assertThat(queue.isEmpty()).isTrue();
        assertThatThrownBy(queue::poll).isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void pollsInTopologicalOrderRegardlessOfInsertionOrder() {
        var queue = new NodeTopologicalOrderQueue(graphWithOrders(30, 10, 20, 0), 4);

        queue.offer(0);
        queue.offer(1);
        queue.offer(2);
        queue.offer(3);

        assertThat(drain(queue)).containsExactly(3, 1, 2, 0);
    }

    /**
     * The queue holds the nodes that were offered, not every node of the graph.
     */
    @Test
    void drainsOnlyTheNodesThatWereOffered() {
        var queue = new NodeTopologicalOrderQueue(graphWithOrders(0, 10, 20, 30, 40), 5);

        queue.offer(1);
        queue.offer(3);

        assertThat(drain(queue)).containsExactly(1, 3);
    }

    @Test
    void offeringANodeAlreadyInTheQueueDoesNothing() {
        var queue = new NodeTopologicalOrderQueue(graphWithOrders(5, 1, 9), 3);

        queue.offer(2);
        queue.offer(0);
        queue.offer(2);
        queue.offer(1);
        queue.offer(0);

        assertThat(drain(queue)).containsExactly(1, 0, 2);
    }

    /**
     * A node that has left the queue may be offered again; only nodes currently in it are deduplicated.
     * The consumer relies on this being allowed, as it re-enters {@link NodeTopologicalOrderQueue#offer(int)}
     * for every forward edge of every node it processes.
     */
    @Test
    void aNodeCanBeOfferedAgainOnceItHasBeenPolled() {
        var queue = new NodeTopologicalOrderQueue(graphWithOrders(0, 1), 2);

        queue.offer(0);
        assertThat(queue.poll()).isZero();

        queue.offer(0);
        queue.offer(1);
        assertThat(drain(queue)).containsExactly(0, 1);
    }

    /**
     * Guards the capacity claim: deduplication bounds the queue at one entry per node,
     * so the backing arrays are never asked to grow.
     */
    @Test
    void holdsEveryNodeAtOnce() {
        var nodeCount = 64;
        var orders = new int[nodeCount];
        for (var node = 0; node < nodeCount; node++) {
            orders[node] = nodeCount - node;
        }
        var queue = new NodeTopologicalOrderQueue(graphWithOrders(orders), nodeCount);

        for (var round = 0; round < 3; round++) { // Offer every node three times over.
            for (var node = 0; node < nodeCount; node++) {
                queue.offer(node);
            }
        }

        assertThat(drain(queue)).hasSize(nodeCount);
    }

    /**
     * Ties are broken arbitrarily, so this only asserts that equal orders come out together.
     */
    @Test
    void nodesSharingATopologicalOrderComeOutTogether() {
        var queue = new NodeTopologicalOrderQueue(graphWithOrders(7, 7, 3, 7), 4);

        queue.offer(0);
        queue.offer(1);
        queue.offer(2);
        queue.offer(3);

        var polled = drain(queue);
        assertThat(polled.getFirst()).isEqualTo(2);
        assertThat(polled.subList(1, 4)).containsExactlyInAnyOrder(0, 1, 3);
    }

    /**
     * The queue replaces a {@link PriorityQueue} of the same nodes, so on a random workload of interleaved
     * offers and polls the two must agree on the sequence of topological orders they hand back.
     */
    @Test
    void matchesAPriorityQueueOnRandomWorkloads() {
        var random = new Random(0);
        var nodeCount = 200;
        for (var attempt = 0; attempt < 50; attempt++) {
            var orders = new int[nodeCount];
            for (var node = 0; node < nodeCount; node++) {
                orders[node] = random.nextInt(nodeCount / 4); // Deliberately many ties.
            }
            var graph = graphWithOrders(orders);
            var queue = new NodeTopologicalOrderQueue(graph, nodeCount);
            var reference = new PriorityQueue<BaseTopologicalOrderGraph.NodeTopologicalOrder>();
            var inReference = new boolean[nodeCount];

            var actualOrders = new ArrayList<Integer>();
            var expectedOrders = new ArrayList<Integer>();
            for (var operation = 0; operation < 1000; operation++) {
                if (random.nextBoolean()) {
                    var node = random.nextInt(nodeCount);
                    queue.offer(node);
                    if (!inReference[node]) { // Mirror the queue's deduplication.
                        reference.add(new BaseTopologicalOrderGraph.NodeTopologicalOrder(node, graph));
                        inReference[node] = true;
                    }
                } else if (!queue.isEmpty()) {
                    var polled = queue.poll();
                    actualOrders.add(orders[polled]);
                    var referenceNode = reference.poll().nodeId();
                    inReference[referenceNode] = false;
                    expectedOrders.add(orders[referenceNode]);
                }
            }
            while (!queue.isEmpty()) {
                actualOrders.add(orders[queue.poll()]);
                expectedOrders.add(orders[reference.poll().nodeId()]);
            }

            assertThat(actualOrders)
                    .as("attempt %d", attempt)
                    .isEqualTo(expectedOrders);
        }
    }

}
