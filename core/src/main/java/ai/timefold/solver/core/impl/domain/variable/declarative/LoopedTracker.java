package ai.timefold.solver.core.impl.domain.variable.declarative;

import java.util.Arrays;

import org.jspecify.annotations.NullMarked;

@NullMarked
public final class LoopedTracker {
    private final LoopedStatus[] statuses;

    public LoopedTracker(int count) {
        statuses = new LoopedStatus[count];
        Arrays.fill(statuses, LoopedStatus.UNKNOWN);
    }

    public void mark(int node, LoopedStatus status) {
        statuses[node] = status;
    }

    public LoopedStatus status(int node) {
        return statuses[node];
    }
}
