package ai.timefold.solver.core.impl.domain.variable.declarative;

import java.util.BitSet;

import org.jspecify.annotations.NullMarked;

@NullMarked
public final class LoopedTracker {

    // Simple LoopedStatus[] array would have occupied too much memory with large node counts.
    // Furthermore, allocating and/or clearing these large arrays is expensive as well.
    private final BitSet present;
    private final BitSet looped;

    public LoopedTracker(int nodeCount) {
        this.present = new BitSet(nodeCount);
        this.looped = new BitSet(nodeCount);
    }

    public void mark(int node, LoopedStatus status) {
        if (status == LoopedStatus.UNKNOWN) {
            present.clear(node);
            looped.clear(node);
        } else {
            present.set(node);
            looped.set(node, status == LoopedStatus.LOOPED);
        }
    }

    public LoopedStatus status(int node) {
        if (present.isEmpty() || !present.get(node)) {
            return LoopedStatus.UNKNOWN;
        }
        return looped.get(node) ? LoopedStatus.LOOPED : LoopedStatus.NOT_LOOPED;
    }

    public void clear() {
        present.clear();
        looped.clear();
    }

}
