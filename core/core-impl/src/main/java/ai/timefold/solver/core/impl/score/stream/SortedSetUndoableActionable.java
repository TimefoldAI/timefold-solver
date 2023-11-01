package ai.timefold.solver.core.impl.score.stream;

import java.util.Comparator;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.SortedSet;
import java.util.TreeMap;

import ai.timefold.solver.core.impl.util.MutableInt;

public final class SortedSetUndoableActionable<Result> implements UndoableActionable<Result, SortedSet<Result>> {
    private final NavigableMap<Result, MutableInt> itemToCount;

    private SortedSetUndoableActionable(NavigableMap<Result, MutableInt> itemToCount) {
        this.itemToCount = itemToCount;
    }

    public static <Result> SortedSetUndoableActionable<Result> orderBy(Comparator<? super Result> comparator) {
        return new SortedSetUndoableActionable<>(new TreeMap<>(comparator));
    }

    @Override
    public Runnable insert(Result result) {
        MutableInt count = itemToCount.computeIfAbsent(result, ignored -> new MutableInt());
        count.increment();
        return () -> {
            if (count.decrement() == 0) {
                itemToCount.remove(result);
            }
        };
    }

    @Override
    public NavigableSet<Result> result() {
        return itemToCount.navigableKeySet();
    }
}
