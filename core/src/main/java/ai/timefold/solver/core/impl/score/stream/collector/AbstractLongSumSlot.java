package ai.timefold.solver.core.impl.score.stream.collector;

public abstract class AbstractLongSumSlot {

    public static final class State {
        long sum = 0;

        public Long result() {
            return sum;
        }
    }

    private final State state;
    private long cachedInput;

    public AbstractLongSumSlot(State state) {
        this.state = state;
    }

    protected void addMapped(long input) {
        cachedInput = input;
        state.sum += input;
    }

    protected void updateMapped(long input) {
        state.sum += input - cachedInput;
        cachedInput = input;
    }

    protected void removeMapped() {
        state.sum -= cachedInput;
    }
}
