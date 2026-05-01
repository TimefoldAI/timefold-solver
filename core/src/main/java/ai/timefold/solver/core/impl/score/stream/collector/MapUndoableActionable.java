package ai.timefold.solver.core.impl.score.stream.collector;

import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BinaryOperator;
import java.util.function.IntFunction;
import java.util.function.Supplier;

import ai.timefold.solver.core.impl.util.Pair;

public final class MapUndoableActionable<Key_, Value_, ResultValue_, Result_ extends Map<Key_, ResultValue_>>
        implements UndoableActionable<Pair<Key_, Value_>> {
    public static final class State<Key_, Value_, ResultValue_, Result_ extends Map<Key_, ResultValue_>> {
        final ToMapResultContainer<Key_, Value_, ResultValue_, Result_> container;

        State(ToMapResultContainer<Key_, Value_, ResultValue_, Result_> container) {
            this.container = container;
        }

        public Result_ result() {
            return container.getResult();
        }
    }

    public static <Key_, Value_, Set_ extends Set<Value_>, Result_ extends Map<Key_, Set_>>
            State<Key_, Value_, Set_, Result_> multiMapState(Supplier<Result_> resultSupplier, IntFunction<Set_> setFunction) {
        return new State<>(new ToMultiMapResultContainer<>(resultSupplier, setFunction));
    }

    public static <Key_, Value_, Result_ extends Map<Key_, Value_>>
            State<Key_, Value_, Value_, Result_>
            mergeMapState(Supplier<Result_> resultSupplier, BinaryOperator<Value_> mergeFunction) {
        return new State<>(new ToSimpleMapResultContainer<>(resultSupplier, mergeFunction));
    }

    private final State<Key_, Value_, ResultValue_, Result_> state;
    private Pair<Key_, Value_> cachedEntry;

    public MapUndoableActionable(State<Key_, Value_, ResultValue_, Result_> state) {
        this.state = state;
    }

    @Override
    public void insert(Pair<Key_, Value_> entry) {
        this.cachedEntry = entry;
        state.container.add(entry.key(), entry.value());
    }

    @Override
    public void update(Pair<Key_, Value_> entry) {
        if (Objects.equals(cachedEntry, entry)) {
            return;
        }
        retract();
        insert(entry);
    }

    @Override
    public void retract() {
        state.container.remove(cachedEntry.key(), cachedEntry.value());
    }
}
