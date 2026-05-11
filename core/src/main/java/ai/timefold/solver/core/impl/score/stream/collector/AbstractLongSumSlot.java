package ai.timefold.solver.core.impl.score.stream.collector;

import ai.timefold.solver.core.impl.util.MutableLong;

public abstract class AbstractLongSumSlot {

    private final MutableLong state;
    private long cachedInput;

    public AbstractLongSumSlot(MutableLong state) {
        this.state = state;
    }

    protected void addMapped(long input) {
        cachedInput = input;
        state.add(input);
    }

    protected void updateMapped(long input) {
        state.add(Math.subtractExact(input, cachedInput));
        cachedInput = input;
    }

    protected void removeMapped() {
        state.subtract(cachedInput);
    }
}
