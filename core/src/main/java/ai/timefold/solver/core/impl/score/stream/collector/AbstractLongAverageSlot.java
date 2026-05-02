package ai.timefold.solver.core.impl.score.stream.collector;

public abstract class AbstractLongAverageSlot {

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

    public AbstractLongAverageSlot(State state) {
        this.state = state;
    }

    protected void addMapped(long input) {
        cachedInput = input;
        state.count++;
        state.sum += input;
    }

    protected void updateMapped(long input) {
        state.sum += input - cachedInput;
        cachedInput = input;
    }

    protected void removeMapped() {
        state.count--;
        state.sum -= cachedInput;
    }
}
