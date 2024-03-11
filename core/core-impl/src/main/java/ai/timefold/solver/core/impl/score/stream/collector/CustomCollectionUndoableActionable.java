package ai.timefold.solver.core.impl.score.stream.collector;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.IntFunction;

public final class CustomCollectionUndoableActionable<Mapped_, Result_ extends Collection<Mapped_>>
        implements UndoableActionable<Mapped_, Result_> {
    private final IntFunction<Result_> collectionFunction;
    private final List<Mapped_> resultList = new ArrayList<>();

    public CustomCollectionUndoableActionable(IntFunction<Result_> collectionFunction) {
        this.collectionFunction = collectionFunction;
    }

    @Override
    public Runnable insert(Mapped_ result) {
        resultList.add(result);
        return () -> resultList.remove(result);
    }

    @Override
    public Result_ result() {
        Result_ out = collectionFunction.apply(resultList.size());
        if (resultList.isEmpty()) {
            // Avoid exception if out is an immutable collection
            return out;
        }
        out.addAll(resultList);
        return out;
    }
}
