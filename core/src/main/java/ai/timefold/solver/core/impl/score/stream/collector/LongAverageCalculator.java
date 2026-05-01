package ai.timefold.solver.core.impl.score.stream.collector;

public final class LongAverageCalculator implements LongCalculator {

    public static final class State {
        long count = 0;
        long sum = 0;

        public Double result() {
            if (count == 0) {
                return null;
            }
            return sum / (double) count;
        }
    }

    private final State state;
    private long cachedInput;

    public LongAverageCalculator(State state) {
        this.state = state;
    }

    @Override
    public void insert(long input) {
        cachedInput = input;
        state.count++;
        state.sum += input;
    }

    @Override
    public void update(long input) {
        state.sum += input - cachedInput;
        cachedInput = input;
    }

    @Override
    public void retract() {
        state.count--;
        state.sum -= cachedInput;
    }
}
