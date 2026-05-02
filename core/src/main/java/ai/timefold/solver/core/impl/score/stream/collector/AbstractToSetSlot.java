package ai.timefold.solver.core.impl.score.stream.collector;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import ai.timefold.solver.core.impl.util.MutableInt;

import org.jspecify.annotations.Nullable;

public abstract class AbstractToSetSlot<Mapped_> {
    public static final class State<Mapped_> {
        private final Map<Mapped_, MutableInt> itemToCount = new LinkedHashMap<>();

        public Set<Mapped_> result() {
            return itemToCount.keySet();
        }
    }

    private final State<Mapped_> state;
    private @Nullable Mapped_ cachedValue;
    private @Nullable MutableInt cachedCount;

    public AbstractToSetSlot(State<Mapped_> state) {
        this.state = state;
    }

    protected void addMapped(Mapped_ result) {
        cachedValue = result;
        cachedCount = state.itemToCount.computeIfAbsent(result, ignored -> new MutableInt());
        cachedCount.increment();
    }

    protected void updateMapped(Mapped_ result) {
        if (Objects.equals(cachedValue, result)) {
            return;
        }
        removeMapped();
        addMapped(result);
    }

    protected void removeMapped() {
        if (cachedCount.decrement() == 0) {
            state.itemToCount.remove(cachedValue);
        }
    }
}
