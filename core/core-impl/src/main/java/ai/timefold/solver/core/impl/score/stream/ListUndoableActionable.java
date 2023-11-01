package ai.timefold.solver.core.impl.score.stream;

import java.util.ArrayList;
import java.util.List;

public final class ListUndoableActionable<Result> implements UndoableActionable<Result, List<Result>> {
    private final List<Result> resultList = new ArrayList<>();

    @Override
    public Runnable insert(Result result) {
        resultList.add(result);
        return () -> resultList.remove(result);
    }

    @Override
    public List<Result> result() {
        return resultList;
    }
}
