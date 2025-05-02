package ai.timefold.solver.core.impl.domain.variable.declarative;

import java.util.Arrays;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public final class LoopedTracker {

    private final @Nullable LoopedStatus[] statuses;

    LoopedTracker(int count) {
        this.statuses = new LoopedStatus[count];
        clear();
    }

    public void mark(int node, LoopedStatus status) {
        statuses[node] = status;
    }

    public LoopedStatus status(int node) {
        var status = statuses[node];
        return status == null ? LoopedStatus.UNKNOWN : status;
    }

    public void clear() {
        // Zeroing-out an array appears to be faster than filling it with a value.
        Arrays.fill(statuses, null);
    }

}
