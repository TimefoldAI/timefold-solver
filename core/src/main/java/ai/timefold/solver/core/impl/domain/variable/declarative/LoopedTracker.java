package ai.timefold.solver.core.impl.domain.variable.declarative;

import static ai.timefold.solver.core.impl.util.DynamicIntArray.ClearingStrategy.PARTIAL;

import ai.timefold.solver.core.impl.util.DynamicIntArray;

import org.jspecify.annotations.NullMarked;

@NullMarked
public final class LoopedTracker {

    // For some reason, the array was getting re-created on every values() call.
    // So, we cache a single instance.
    private static final LoopedStatus[] VALUES = LoopedStatus.values();

    private final DynamicIntArray looped;

    public LoopedTracker(int nodeCount) {
        // We never fully clear the array, as that was shown to cause too much GC pressure.
        this.looped = new DynamicIntArray(nodeCount, PARTIAL);
    }

    public void mark(int node, LoopedStatus status) {
        looped.set(node, status.ordinal());
    }

    public LoopedStatus status(int node) {
        // When in the unallocated part of the dynamic array, the value returned is zero.
        // Therefore it is imperative that LoopedStatus.UNKNOWN be the first element in the enum.
        return VALUES[looped.get(node)];
    }

    public void clear() {
        looped.clear();
    }

}
