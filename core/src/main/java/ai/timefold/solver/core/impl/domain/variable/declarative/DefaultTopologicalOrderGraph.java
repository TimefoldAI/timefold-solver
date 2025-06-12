package ai.timefold.solver.core.impl.domain.variable.declarative;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PrimitiveIterator;
import java.util.Set;
import java.util.stream.Collectors;

import ai.timefold.solver.core.impl.util.CollectionUtils;
import ai.timefold.solver.core.impl.util.MutableInt;

public class DefaultTopologicalOrderGraph implements TopologicalOrderGraph {

    private final NodeTopologicalOrder[] nodeIdToTopologicalOrderMap;
    private final Map<Integer, List<Integer>> componentMap;
    private final Set<Integer>[] forwardEdges;
    private final Set<Integer>[] backEdges;
    private final boolean[] isNodeInLoopedComponent;

    @SuppressWarnings({ "unchecked" })
    public DefaultTopologicalOrderGraph(final int size) {
        this.nodeIdToTopologicalOrderMap = new NodeTopologicalOrder[size];
        this.componentMap = CollectionUtils.newLinkedHashMap(size);
        this.forwardEdges = new Set[size];
        this.backEdges = new Set[size];
        this.isNodeInLoopedComponent = new boolean[size];
        for (var i = 0; i < size; i++) {
            forwardEdges[i] = new HashSet<>();
            backEdges[i] = new HashSet<>();
            isNodeInLoopedComponent[i] = false;
            nodeIdToTopologicalOrderMap[i] = new NodeTopologicalOrder(i, i);
        }
    }

    List<Integer> getComponent(int node) {
        return componentMap.get(node);
    }

    @Override
    public void addEdge(int fromNode, int toNode) {
        forwardEdges[fromNode].add(toNode);
        backEdges[toNode].add(fromNode);
    }

    @Override
    public void removeEdge(int fromNode, int toNode) {
        forwardEdges[fromNode].remove(toNode);
        backEdges[toNode].remove(fromNode);
    }

    @Override
    public void forEachEdge(EdgeConsumer edgeConsumer) {
        for (var fromNode = 0; fromNode < forwardEdges.length; fromNode++) {
            for (var toNode : forwardEdges[fromNode]) {
                edgeConsumer.accept(fromNode, toNode);
            }
        }
    }

    @Override
    public PrimitiveIterator.OfInt nodeForwardEdges(int fromNode) {
        return componentMap.get(fromNode).stream()
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
    public NodeTopologicalOrder getTopologicalOrder(int node) {
        return nodeIdToTopologicalOrderMap[node];
    }

    @Override
    public void commitChanges(BitSet changed) {
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
            var componentSize = component.cardinality();
            var isComponentLooped = componentSize != 1;
            var componentNodes = new ArrayList<Integer>(componentSize);
            for (var node = component.nextSetBit(0); node >= 0; node = component.nextSetBit(node + 1)) {
                nodeIdToTopologicalOrderMap[node] = new NodeTopologicalOrder(node, ordIndex);
                componentNodes.add(node);
                componentMap.put(node, componentNodes);

                if (isComponentLooped != isNodeInLoopedComponent[node]) {
                    // It is enough to only mark nodes whose component
                    // status changed; the updater will notify descendants
                    // since a looped status change force updates descendants.
                    isNodeInLoopedComponent[node] = isComponentLooped;
                    changed.set(node);
                }
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

    @Override
    public String toString() {
        var out = new StringBuilder();
        out.append("DefaultTopologicalOrderGraph{\n");
        for (var node = 0; node < forwardEdges.length; node++) {
            out.append("    ").append(node).append("(").append(nodeIdToTopologicalOrderMap[node].order()).append(") -> ")
                    .append(forwardEdges[node].stream()
                            .sorted()
                            .map(Object::toString)
                            .collect(Collectors.joining(",", "[", "]\n")));
        }
        out.append("}");
        return out.toString();
    }
}
