package ai.timefold.solver.core.impl.score.stream.collector;

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
