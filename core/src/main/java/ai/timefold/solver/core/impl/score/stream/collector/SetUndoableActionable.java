package ai.timefold.solver.core.impl.score.stream.collector;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import ai.timefold.solver.core.impl.util.MutableInt;

public final class SetUndoableActionable<Mapped_> implements UndoableActionable<Mapped_> {
    public static final class State<Mapped_> {
        final Map<Mapped_, MutableInt> itemToCount = new LinkedHashMap<>();

        public Set<Mapped_> result() {
            return itemToCount.keySet();
        }
    }

    private final State<Mapped_> state;
    private Mapped_ cachedValue;
    private MutableInt cachedCount;

    public SetUndoableActionable(State<Mapped_> state) {
        this.state = state;
    }

    @Override
    public void insert(Mapped_ result) {
        this.cachedValue = result;
        this.cachedCount = state.itemToCount.computeIfAbsent(result, ignored -> new MutableInt());
        this.cachedCount.increment();
    }

    @Override
    public void update(Mapped_ result) {
        if (Objects.equals(cachedValue, result)) {
            return;
        }
        retract();
        insert(result);
    }

    @Override
    public void retract() {
        if (cachedCount.decrement() == 0) {
            state.itemToCount.remove(cachedValue);
        }
    }
}
