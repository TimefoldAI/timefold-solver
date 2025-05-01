package ai.timefold.solver.core.impl.domain.variable.declarative;

import java.util.Arrays;

import org.jspecify.annotations.NullMarked;

@NullMarked
public final class LoopedTracker {

    private final LoopedStatus[] statuses;

    LoopedTracker(int count) {
        this.statuses = new LoopedStatus[count];
        clear();
    }

    public void mark(int node, LoopedStatus status) {
        statuses[node] = status;
    }

    public LoopedStatus status(int node) {
        return statuses[node];
    }

    public void clear() {
        Arrays.fill(statuses, LoopedStatus.UNKNOWN);
    }

}
