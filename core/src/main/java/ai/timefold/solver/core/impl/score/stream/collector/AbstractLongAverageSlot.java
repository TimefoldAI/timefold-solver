package ai.timefold.solver.core.impl.score.stream.collector;

public abstract class AbstractLongAverageSlot {

    public static final class State {
        private long count = 0;
        private long sum = 0;

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
        state.sum = Math.addExact(state.sum, input);
    }

    protected void replaceWithMapped(long input) {
        state.sum += Math.subtractExact(input, cachedInput);
        cachedInput = input;
    }

    protected void removeMapped() {
        state.count--;
        state.sum = Math.subtractExact(state.sum, cachedInput);
    }
}
