package ai.timefold.solver.core.impl.domain.variable.declarative;

import static ai.timefold.solver.core.impl.util.DynamicIntArray.ClearingStrategy.PARTIAL;

import java.util.Arrays;

import ai.timefold.solver.core.impl.util.DynamicIntArray;

import org.jspecify.annotations.NullMarked;

@NullMarked
public final class LoopedTracker {

    // For some reason, the array was getting re-created on every values() call.
    // So, we cache a single instance.
    private static final LoopedStatus[] VALUES = LoopedStatus.values();

    private final DynamicIntArray looped;
    private final int[][] entityIdToNodes;

    // Needed so we can update all nodes in a cycle or depended on a cycle
    // (only nodes that formed a cycle get marked as changed by the graph;
    // their dependents don't)
    private final boolean[] entityLoopedStatusChanged;

    public LoopedTracker(int nodeCount, int[][] entityIdToNodes) {
        this.entityIdToNodes = entityIdToNodes;
        this.entityLoopedStatusChanged = new boolean[entityIdToNodes.length];
        // We never fully clear the array, as that was shown to cause too much GC pressure.
        this.looped = new DynamicIntArray(nodeCount, PARTIAL);
    }

    public void mark(int node, LoopedStatus status) {
        looped.set(node, status.ordinal());
    }

    public boolean isEntityLooped(BaseTopologicalOrderGraph graph, int entityId, boolean wasEntityLooped) {
        for (var entityNode : entityIdToNodes[entityId]) {
            if (graph.isLooped(this, entityNode)) {
                if (!wasEntityLooped) {
                    entityLoopedStatusChanged[entityId] = true;
                }
                return true;
            }
        }
        if (wasEntityLooped) {
            entityLoopedStatusChanged[entityId] = true;
        }
        return false;
    }

    public boolean didEntityLoopedStatusChange(int entityId) {
        return entityLoopedStatusChanged[entityId];
    }

    public LoopedStatus status(int node) {
        // When in the unallocated part of the dynamic array, the value returned is zero.
        // Therefore it is imperative that LoopedStatus.UNKNOWN be the first element in the enum.
        return VALUES[looped.get(node)];
    }

    public void clear() {
        Arrays.fill(entityLoopedStatusChanged, false);
        looped.clear();
    }

}
