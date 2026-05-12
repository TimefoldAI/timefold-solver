package ai.timefold.solver.core.impl.score.stream.collector;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import ai.timefold.solver.core.impl.util.MutableInt;

import org.jspecify.annotations.Nullable;

public abstract class AbstractLongDistinctSlot<Input_> {

    public static final class State<Input_> {

        private final Map<Input_, MutableInt> countMap = new HashMap<>();

        public Long result() {
            return (long) countMap.size();
        }

    }

    private final State<Input_> state;
    private @Nullable Input_ cachedInput;
    private @Nullable MutableInt cachedCounter;

    public AbstractLongDistinctSlot(State<Input_> state) {
        this.state = state;
    }

    protected void addMapped(Input_ input) {
        cachedInput = input;
        cachedCounter = state.countMap.computeIfAbsent(input, ignored -> new MutableInt());
        cachedCounter.increment();
    }

    protected void replaceWithMapped(Input_ input) {
        if (Objects.equals(cachedInput, input)) {
            return;
        }
        removeMapped();
        addMapped(input);
    }

    protected void removeMapped() {
        if (cachedCounter.decrement() == 0) {
            state.countMap.remove(cachedInput);
        }
    }
}
