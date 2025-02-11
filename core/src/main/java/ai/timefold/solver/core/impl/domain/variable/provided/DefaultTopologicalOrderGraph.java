package ai.timefold.solver.core.impl.domain.variable.provided;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashSet;
import java.util.List;
import java.util.PrimitiveIterator;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

public class DefaultTopologicalOrderGraph implements TopologicalOrderGraph {
    private final int[] ord;
    private final boolean[] loopedComponent;
    private final Set<Integer>[] forwardEdges;
    private final Set<Integer>[] backEdges;

    @SuppressWarnings({ "unchecked" })
    public DefaultTopologicalOrderGraph(final int size) {
        this.ord = new int[size];
        this.loopedComponent = new boolean[size];
        this.forwardEdges = new Set[size];
        this.backEdges = new Set[size];
        for (int i = 0; i < size; i++) {
            forwardEdges[i] = new HashSet<>();
            backEdges[i] = new HashSet<>();
            ord[i] = i;
        }
    }

    @Override
    public void addEdge(int from, int to) {
        forwardEdges[from].add(to);
        backEdges[to].add(from);
    }

    @Override
    public void removeEdge(int from, int to) {
        forwardEdges[from].remove(to);
        backEdges[to].remove(from);
    }

    @Override
    public PrimitiveIterator.OfInt componentForwardEdges(int from) {
        return forwardEdges[from].stream().mapToInt(Integer::intValue).iterator();
    }

    @Override
    public boolean isLooped(LoopedTracker loopedTracker, int node) {
        return switch (loopedTracker.status(node)) {
            case UNKNOWN -> {
                if (loopedComponent[node]) {
                    loopedTracker.mark(node, LoopedStatus.LOOPED);
                    yield true;
                }
                for (var backEdge : backEdges[node]) {
                    if (isLooped(loopedTracker, backEdge)) {
                        loopedTracker.mark(node, LoopedStatus.LOOPED);
                        yield true;
                    }
                }
                loopedTracker.mark(node, LoopedStatus.NOT_LOOPED);
                yield false;
            }
            case NOT_LOOPED -> false;
            case LOOPED -> true;
        };
    }

    @Override
    public int getTopologicalOrder(int node) {
        return ord[node];
    }

    @Override
    public void endBatchChange() {
        AtomicInteger index = new AtomicInteger(1);
        AtomicInteger stackIndex = new AtomicInteger(0);
        int size = forwardEdges.length;
        int[] stack = new int[size];
        int[] indexMap = new int[size];
        int[] lowMap = new int[size];
        boolean[] onStackSet = new boolean[size];
        List<BitSet> components = new ArrayList<>();

        for (int node = 0; node < size; node++) {
            if (indexMap[node] == 0) {
                strongConnect(node, index, stackIndex, stack, indexMap, lowMap, onStackSet, components);
            }
        }

        int ordIndex = 0;
        for (int i = components.size() - 1; i >= 0; i--) {
            var component = components.get(i);
            for (int node = component.nextSetBit(0); node >= 0; node = component.nextSetBit(node + 1)) {
                ord[node] = ordIndex;
                loopedComponent[node] = component.cardinality() > 1;
                ordIndex++;
                if (node == Integer.MAX_VALUE) {
                    break;
                }
            }
        }
    }

    private void strongConnect(int node, AtomicInteger index, AtomicInteger stackIndex, int[] stack,
            int[] indexMap,
            int[] lowMap, boolean[] onStackSet, List<BitSet> components) {
        // Set the depth index for node to the smallest unused index
        indexMap[node] = index.get();
        lowMap[node] = index.get();
        index.getAndIncrement();
        stack[stackIndex.getAndIncrement()] = node;
        onStackSet[node] = true;

        // Consider successors of node
        for (int successor : forwardEdges[node]) {
            if (indexMap[successor] == 0) {
                // Successor has not yet been visited; recurse on it
                strongConnect(successor, index, stackIndex, stack, indexMap, lowMap, onStackSet, components);
                lowMap[node] = Math.min(lowMap[node], lowMap[successor]);
            } else if (onStackSet[successor]) {
                // Successor is in stack S and hence in the current SCC
                // If successor is not on stack, then (node, successor) is an edge pointing to an SCC already found and must be ignored
                // The next line may look odd - but is correct.
                // It says successor.index not successor.low; that is deliberate and from the original paper
                lowMap[node] = Math.min(lowMap[node], indexMap[successor]);
            }
        }

        // If node is a root node, pop the stack and generate an SCC
        if (onStackSet[node] && lowMap[node] == indexMap[node]) {
            BitSet out = new BitSet();

            int current;
            do {
                current = stack[stackIndex.decrementAndGet()];
                onStackSet[current] = false;
                out.set(current);
            } while (node != current);
            components.add(out);
        }
    }
}
