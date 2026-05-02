package ai.timefold.solver.core.impl.score.stream.collector;

public abstract class AbstractCountSlot {

    public static final class State {
        private long count = 0;

        public Long result() {
            return count;
        }
    }

    private final State state;

    public AbstractCountSlot(State state) {
        this.state = state;
    }

    protected void addMapped() {
        state.count++;
    }

    protected void updateMapped() {
        // count unchanged on update
    }

    protected void removeMapped() {
        state.count--;
    }
}
