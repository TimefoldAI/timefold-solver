package ai.timefold.solver.core.impl.score.stream.collector.bi;

import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Supplier;

import ai.timefold.solver.core.api.score.stream.bi.BiConstraintCollectorAccumulatedValue;
import ai.timefold.solver.core.impl.score.stream.collector.AbstractToMapSlot;

import org.jspecify.annotations.NonNull;

final class ToMultiMapBiCollector<A, B, Key_, Value_, Set_ extends Set<Value_>, Result_ extends Map<Key_, Set_>>
        extends
        UndoableActionableBiCollector<A, B, Key_, Result_, AbstractToMapSlot.State<Key_, Value_, Set_, Result_>> {
    private final BiFunction<? super A, ? super B, ? extends Key_> keyFunction;
    private final BiFunction<? super A, ? super B, ? extends Value_> valueFunction;
    private final Supplier<Result_> mapSupplier;
    private final IntFunction<Set_> setFunction;

    ToMultiMapBiCollector(BiFunction<? super A, ? super B, ? extends Key_> keyFunction,
            BiFunction<? super A, ? super B, ? extends Value_> valueFunction,
            Supplier<Result_> mapSupplier,
            IntFunction<Set_> setFunction) {
        super(keyFunction);
        this.keyFunction = keyFunction;
        this.valueFunction = valueFunction;
        this.mapSupplier = mapSupplier;
        this.setFunction = setFunction;
    }

    @Override
    public @NonNull Supplier<AbstractToMapSlot.State<Key_, Value_, Set_, Result_>> supplier() {
        return () -> AbstractToMapSlot.multiMapState(mapSupplier, setFunction);
    }

    @Override
    public @NonNull Function<AbstractToMapSlot.State<Key_, Value_, Set_, Result_>, Result_> finisher() {
        return state -> state.result();
    }

    @Override
    protected BiConstraintCollectorAccumulatedValue<A, B> newAccumulatedValue(
            AbstractToMapSlot.State<Key_, Value_, Set_, Result_> state) {
        return new Slot(state);
    }

    private final class Slot extends AbstractToMapSlot<Key_, Value_, Set_, Result_>
            implements BiConstraintCollectorAccumulatedValue<A, B> {
        Slot(AbstractToMapSlot.State<Key_, Value_, Set_, Result_> state) {
            super(state);
        }

        @Override
        public void add(A a, B b) {
            addMapped(keyFunction.apply(a, b), valueFunction.apply(a, b));
        }

        @Override
        public void update(A a, B b) {
            updateMapped(keyFunction.apply(a, b), valueFunction.apply(a, b));
        }

        @Override
        public void remove() {
            removeMapped();
        }
    }

    // Don't call super equals/hashCode; the groupingFunction is calculated from keyFunction
    // and valueFunction
    @Override
    public boolean equals(Object object) {
        if (this == object)
            return true;
        if (object == null || getClass() != object.getClass())
            return false;
        var that = (ToMultiMapBiCollector<?, ?, ?, ?, ?, ?>) object;
        return Objects.equals(keyFunction, that.keyFunction) && Objects.equals(valueFunction,
                that.valueFunction) && Objects.equals(mapSupplier, that.mapSupplier)
                && Objects.equals(
                        setFunction, that.setFunction);
    }

    @Override
    public int hashCode() {
        return Objects.hash(keyFunction, valueFunction, mapSupplier, setFunction);
    }
}
