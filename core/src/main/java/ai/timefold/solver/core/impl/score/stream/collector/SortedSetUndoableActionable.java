package ai.timefold.solver.core.impl.score.stream.collector;

import java.util.Comparator;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.SortedSet;
import java.util.TreeMap;

import ai.timefold.solver.core.impl.util.MutableInt;

public final class SortedSetUndoableActionable<Mapped_> implements UndoableActionable<Mapped_, SortedSet<Mapped_>> {
    private final NavigableMap<Mapped_, MutableInt> itemToCount;

    private SortedSetUndoableActionable(NavigableMap<Mapped_, MutableInt> itemToCount) {
        this.itemToCount = itemToCount;
    }

    public static <Result> SortedSetUndoableActionable<Result> orderBy(Comparator<? super Result> comparator) {
        return new SortedSetUndoableActionable<>(new TreeMap<>(comparator));
    }

    @Override
    public Runnable insert(Mapped_ result) {
        MutableInt count = itemToCount.computeIfAbsent(result, ignored -> new MutableInt());
        count.increment();
        return () -> {
            if (count.decrement() == 0) {
                itemToCount.remove(result);
            }
        };
    }

    @Override
    public NavigableSet<Mapped_> result() {
        return itemToCount.navigableKeySet();
    }
}
