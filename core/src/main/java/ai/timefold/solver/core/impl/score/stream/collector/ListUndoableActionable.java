package ai.timefold.solver.core.impl.score.stream.collector;

import java.util.List;

import ai.timefold.solver.core.impl.util.ElementAwareArrayList;

public final class ListUndoableActionable<Mapped_> implements UndoableActionable<Mapped_, List<Mapped_>> {

    /**
     * As long as additions and removals are performed using entry-based methods,
     * removals have O(1) performance.
     */
    private final ElementAwareArrayList<Mapped_> resultList = new ElementAwareArrayList<>();

    @Override
    public Runnable insert(Mapped_ result) {
        var entry = resultList.addEntry(result);
        return entry::remove;
    }

    @Override
    public List<Mapped_> result() {
        return resultList;
    }
}
