package ai.timefold.solver.core.impl.bavet.common.collector;

import java.util.ArrayList;
import java.util.List;

public final class ListUndoableActionable<Mapped_> implements UndoableActionable<Mapped_, List<Mapped_>> {
    private final List<Mapped_> resultList = new ArrayList<>();

    @Override
    public Runnable insert(Mapped_ result) {
        resultList.add(result);
        return () -> resultList.remove(result);
    }

    @Override
    public List<Mapped_> result() {
        return resultList;
    }
}
