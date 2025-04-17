package ai.timefold.solver.core.impl.domain.variable.declarative;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PrimitiveIterator;
import java.util.Set;

import ai.timefold.solver.core.impl.util.CollectionUtils;
import ai.timefold.solver.core.impl.util.MutableInt;

public class DefaultTopologicalOrderGraph implements TopologicalOrderGraph {
    private final int[] ord;
    private final Map<Integer, List<Integer>> componentMap;
    private final Set<Integer>[] forwardEdges;
    private final Set<Integer>[] backEdges;

    @SuppressWarnings({ "unchecked" })
    public DefaultTopologicalOrderGraph(final int size) {
        this.ord = new int[size];
        this.componentMap = CollectionUtils.newLinkedHashMap(size);
        this.forwardEdges = new Set[size];
        this.backEdges = new Set[size];
        for (var i = 0; i < size; i++) {
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
    public PrimitiveIterator.OfInt nodeForwardEdges(int from) {
        return componentMap.get(from).stream()
                .flatMap(member -> forwardEdges[member].stream())
                .mapToInt(Integer::intValue)
                .distinct().iterator();
    }

    @Override
    public boolean isLooped(LoopedTracker loopedTracker, int node) {
        return switch (loopedTracker.status(node)) {
            case UNKNOWN -> {
                if (componentMap.get(node).size() > 1) {
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
        var index = new MutableInt(1);
        var stackIndex = new MutableInt(0);
        var size = forwardEdges.length;
        var stack = new int[size];
        var indexMap = new int[size];
        var lowMap = new int[size];
        var onStackSet = new boolean[size];
        var components = new ArrayList<BitSet>();
        componentMap.clear();

        for (var node = 0; node < size; node++) {
            if (indexMap[node] == 0) {
                strongConnect(node, index, stackIndex, stack, indexMap, lowMap, onStackSet, components);
            }
        }

        var ordIndex = 0;
        for (var i = components.size() - 1; i >= 0; i--) {
            var component = components.get(i);
            var componentNodes = new ArrayList<Integer>(component.cardinality());
            for (var node = component.nextSetBit(0); node >= 0; node = component.nextSetBit(node + 1)) {
                ord[node] = ordIndex;
                componentNodes.add(node);
                componentMap.put(node, componentNodes);
                ordIndex++;
                if (node == Integer.MAX_VALUE) {
                    break;
                }
            }
        }
    }

    private void strongConnect(int node, MutableInt index, MutableInt stackIndex, int[] stack,
            int[] indexMap,
            int[] lowMap, boolean[] onStackSet, List<BitSet> components) {
        // Set the depth index for node to the smallest unused index
        indexMap[node] = index.intValue();
        lowMap[node] = index.intValue();
        index.increment();
        stack[stackIndex.intValue()] = node;
        onStackSet[node] = true;
        stackIndex.increment();

        // Consider successors of node
        for (var successor : forwardEdges[node]) {
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
            var out = new BitSet();

            int current;
            do {
                current = stack[stackIndex.decrement()];
                onStackSet[current] = false;
                out.set(current);
            } while (node != current);
            components.add(out);
        }
    }
}
