package ai.timefold.solver.core.impl.score.stream;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.IntFunction;

public final class CustomCollectionUndoableActionable<Mapped, Result extends Collection<Mapped>>
        implements UndoableActionable<Mapped, Result> {
    private final IntFunction<Result> collectionFunction;
    private final List<Mapped> resultList = new ArrayList<>();

    public CustomCollectionUndoableActionable(IntFunction<Result> collectionFunction) {
        this.collectionFunction = collectionFunction;
    }

    @Override
    public Runnable insert(Mapped result) {
        resultList.add(result);
        return () -> resultList.remove(result);
    }

    @Override
    public Result result() {
        Result out = collectionFunction.apply(resultList.size());
        if (resultList.isEmpty()) {
            // Avoid exception if out is an immutable collection
            return out;
        }
        out.addAll(resultList);
        return out;
    }
}
