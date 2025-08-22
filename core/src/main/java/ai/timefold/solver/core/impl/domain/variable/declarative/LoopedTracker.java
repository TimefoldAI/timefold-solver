package ai.timefold.solver.core.impl.domain.variable.declarative;

import static ai.timefold.solver.core.impl.util.DynamicIntArray.ClearingStrategy.PARTIAL;

import java.util.Arrays;

import ai.timefold.solver.core.impl.util.DynamicIntArray;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

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
    private final boolean[] entityInconsistentStatusChanged;

    public LoopedTracker(int nodeCount, int[][] entityIdToNodes) {
        this.entityIdToNodes = entityIdToNodes;
        this.entityInconsistentStatusChanged = new boolean[entityIdToNodes.length];
        // We never fully clear the array, as that was shown to cause too much GC pressure.
        this.looped = new DynamicIntArray(nodeCount, PARTIAL);
    }

    public void mark(int node, LoopedStatus status) {
        looped.set(node, status.ordinal());
    }

    public boolean isEntityInconsistent(BaseTopologicalOrderGraph graph, int entityId,
            @Nullable Boolean wasEntityInconsistent) {
        for (var entityNode : entityIdToNodes[entityId]) {
            if (graph.isLooped(this, entityNode)) {
                if (wasEntityInconsistent == null || !wasEntityInconsistent) {
                    entityInconsistentStatusChanged[entityId] = true;
                }
                return true;
            }
        }
        if (wasEntityInconsistent == null || wasEntityInconsistent) {
            entityInconsistentStatusChanged[entityId] = true;
        }
        return false;
    }

    public boolean didEntityInconsistentStatusChange(int entityId) {
        return entityInconsistentStatusChanged[entityId];
    }

    public LoopedStatus status(int node) {
        // When in the unallocated part of the dynamic array, the value returned is zero.
        // Therefore it is imperative that LoopedStatus.UNKNOWN be the first element in the enum.
        return VALUES[looped.get(node)];
    }

    public void clear() {
        Arrays.fill(entityInconsistentStatusChanged, false);
        looped.clear();
    }

}
