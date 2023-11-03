package ai.timefold.solver.core.impl.score.stream;

public final class IntCounter {
    private int count;

    public void increment() {
        count++;
    }

    public void decrement() {
        count--;
    }

    public int result() {
        return count;
    }
}
