package ai.timefold.solver.core.impl.score.stream.collector;

import java.util.Map;
import java.util.Set;
import java.util.function.BinaryOperator;
import java.util.function.IntFunction;
import java.util.function.Supplier;

import ai.timefold.solver.core.impl.util.Pair;

public final class MapUndoableActionable<Key_, Value_, ResultValue_, Result_ extends Map<Key_, ResultValue_>>
        implements UndoableActionable<Pair<Key_, Value_>, Result_> {
    ToMapResultContainer<Key_, Value_, ResultValue_, Result_> container;

    private MapUndoableActionable(ToMapResultContainer<Key_, Value_, ResultValue_, Result_> container) {
        this.container = container;
    }

    public static <Key_, Value_, Set_ extends Set<Value_>, Result_ extends Map<Key_, Set_>>
            MapUndoableActionable<Key_, Value_, Set_, Result_> multiMap(
                    Supplier<Result_> resultSupplier, IntFunction<Set_> setFunction) {
        return new MapUndoableActionable<>(new ToMultiMapResultContainer<>(resultSupplier, setFunction));
    }

    public static <Key_, Value_, Result_ extends Map<Key_, Value_>> MapUndoableActionable<Key_, Value_, Value_, Result_>
            mergeMap(
                    Supplier<Result_> resultSupplier, BinaryOperator<Value_> mergeFunction) {
        return new MapUndoableActionable<>(new ToSimpleMapResultContainer<>(resultSupplier, mergeFunction));
    }

    @Override
    public Runnable insert(Pair<Key_, Value_> entry) {
        container.add(entry.key(), entry.value());
        return () -> container.remove(entry.key(), entry.value());
    }

    @Override
    public Result_ result() {
        return container.getResult();
    }
}
