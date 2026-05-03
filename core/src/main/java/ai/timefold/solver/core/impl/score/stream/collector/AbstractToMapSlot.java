package ai.timefold.solver.core.impl.score.stream.collector;

import java.util.Map;
import java.util.Set;
import java.util.function.BinaryOperator;
import java.util.function.IntFunction;
import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;

public abstract class AbstractToMapSlot<Key_, Value_, ResultValue_, Result_ extends Map<Key_, ResultValue_>> {
    public static final class State<Key_, Value_, ResultValue_, Result_ extends Map<Key_, ResultValue_>> {
        private final ToMapResultContainer<Key_, Value_, ResultValue_, Result_> container;

        private State(ToMapResultContainer<Key_, Value_, ResultValue_, Result_> container) {
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
    private @Nullable ToMapPerKeyCounter<Key_, Value_> cachedCounter;
    private @Nullable CountHolder<Value_> cachedHolder;

    public AbstractToMapSlot(State<Key_, Value_, ResultValue_, Result_> state) {
        this.state = state;
    }

    protected void addMapped(Key_ key, Value_ value) {
        state.container.add(key, value);
        cachedCounter = state.container.lastCounter();
        cachedHolder = state.container.lastHolder();
    }

    protected void updateMapped(Key_ key, Value_ value) {
        state.container.update(cachedCounter, cachedHolder, key, value);
        cachedCounter = state.container.lastCounter();
        cachedHolder = state.container.lastHolder();
    }

    protected void removeMapped() {
        state.container.remove(cachedCounter, cachedHolder);
    }
}
