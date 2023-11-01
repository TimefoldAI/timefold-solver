package ai.timefold.solver.core.impl.score.stream;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import ai.timefold.solver.core.impl.util.MutableInt;

public final class SetUndoableActionable<Result> implements UndoableActionable<Result, Set<Result>> {
    final Map<Result, MutableInt> itemToCount = new LinkedHashMap<>();

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
    public Set<Result> result() {
        return itemToCount.keySet();
    }
}
