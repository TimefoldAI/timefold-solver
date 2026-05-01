package ai.timefold.solver.core.impl.score.stream.collector;

import java.util.Comparator;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.Objects;
import java.util.TreeMap;

import ai.timefold.solver.core.impl.util.MutableInt;

public final class SortedSetUndoableActionable<Mapped_> implements UndoableActionable<Mapped_> {
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

    public SortedSetUndoableActionable(State<Mapped_> state) {
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
