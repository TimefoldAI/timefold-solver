package ai.timefold.solver.core.impl.score.stream.collector;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import ai.timefold.solver.core.impl.util.MutableInt;

public final class SetUndoableActionable<Mapped_> implements UndoableActionable<Mapped_, Set<Mapped_>> {
    final Map<Mapped_, MutableInt> itemToCount = new LinkedHashMap<>();

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
    public Set<Mapped_> result() {
        return itemToCount.keySet();
    }
}
