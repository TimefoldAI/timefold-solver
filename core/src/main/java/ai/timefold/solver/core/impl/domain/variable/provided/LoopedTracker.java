package ai.timefold.solver.core.impl.domain.variable.provided;

import java.util.Arrays;

import org.jspecify.annotations.NonNull;

public final class LoopedTracker {
    private final LoopedStatus[] statuses;

    public LoopedTracker(int count) {
        statuses = new LoopedStatus[count];
        Arrays.fill(statuses, LoopedStatus.UNKNOWN);
    }

    public void mark(int node, @NonNull LoopedStatus status) {
        statuses[node] = status;
    }

    public @NonNull LoopedStatus status(int node) {
        return statuses[node];
    }
}
