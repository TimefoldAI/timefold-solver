package ai.timefold.solver.core.impl.bavet.common.collector;

public final class LongCounter {
    private long count;

    public void increment() {
        count++;
    }

    public void decrement() {
        count--;
    }

    public long result() {
        return count;
    }
}
