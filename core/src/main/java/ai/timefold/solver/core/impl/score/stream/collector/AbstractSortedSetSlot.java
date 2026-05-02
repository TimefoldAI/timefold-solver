package ai.timefold.solver.core.impl.score.stream.collector;

import java.util.Comparator;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.Objects;
import java.util.TreeMap;

import ai.timefold.solver.core.impl.util.MutableInt;

public abstract class AbstractSortedSetSlot<Mapped_> {
    public static final class State<Mapped_> {
        final NavigableMap<Mapped_, MutableInt> itemToCount;

        public State(Comparator<? super Mapped_> comparator) {
            this.itemToCount = new TreeMap<>(comparator);
        }

        public NavigableSet<Mapped_> result() {
            return itemToCount.navigableKeySet();
        }
    }

    private final State<Mapped_> state;
    private Mapped_ cachedValue;
    private MutableInt cachedCount;

    public AbstractSortedSetSlot(State<Mapped_> state) {
        this.state = state;
    }

    protected void addMapped(Mapped_ result) {
        this.cachedValue = result;
        this.cachedCount = state.itemToCount.computeIfAbsent(result, ignored -> new MutableInt());
        this.cachedCount.increment();
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
