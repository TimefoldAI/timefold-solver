package ai.timefold.solver.core.impl.score.stream.collector;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import ai.timefold.solver.core.impl.util.MutableInt;

import org.jspecify.annotations.Nullable;

public final class LongDistinctCountCalculator<Input_> implements ObjectCalculator<Input_> {

    public static final class State<Input_> {
        private final Map<Input_, MutableInt> countMap = new HashMap<>();

        public Long result() {
            return (long) countMap.size();
        }
    }

    private final State<Input_> state;
    private @Nullable Input_ cachedInput;
    private @Nullable MutableInt cachedCounter;

    public LongDistinctCountCalculator(State<Input_> state) {
        this.state = state;
    }

    @Override
    public void insert(Input_ input) {
        cachedInput = input;
        cachedCounter = state.countMap.computeIfAbsent(input, ignored -> new MutableInt());
        cachedCounter.increment();
    }

    @Override
    public void update(Input_ input) {
        if (Objects.equals(cachedInput, input)) {
            return;
        }
        retract();
        insert(input);
    }

    @Override
    public void retract() {
        if (cachedCounter.decrement() == 0) {
            state.countMap.remove(cachedInput);
        }
    }
}
