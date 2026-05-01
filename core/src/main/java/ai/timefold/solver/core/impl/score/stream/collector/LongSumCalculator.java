package ai.timefold.solver.core.impl.score.stream.collector;

public final class LongSumCalculator implements LongCalculator {

    public static final class State {
        long sum = 0;

        public Long result() {
            return sum;
        }
    }

    private final State state;
    private long cachedInput;

    public LongSumCalculator(State state) {
        this.state = state;
    }

    @Override
    public void insert(long input) {
        cachedInput = input;
        state.sum += input;
    }

    @Override
    public void update(long input) {
        state.sum += input - cachedInput;
        cachedInput = input;
    }

    @Override
    public void retract() {
        state.sum -= cachedInput;
    }
}
