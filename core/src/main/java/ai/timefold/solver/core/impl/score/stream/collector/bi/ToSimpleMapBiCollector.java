package ai.timefold.solver.core.impl.score.stream.collector.bi;

import java.util.Map;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;

import ai.timefold.solver.core.api.score.stream.bi.BiConstraintCollectorValueHandle;
import ai.timefold.solver.core.impl.score.stream.collector.AbstractToMapSlot;

import org.jspecify.annotations.NonNull;

final class ToSimpleMapBiCollector<A, B, Key_, Value_, Result_ extends Map<Key_, Value_>>
        extends AbstractReferenceBasedBiCollector<A, B, Key_, Result_, AbstractToMapSlot.State<Key_, Value_, Value_, Result_>> {
    private final BiFunction<? super A, ? super B, ? extends Key_> keyFunction;
    private final BiFunction<? super A, ? super B, ? extends Value_> valueFunction;
    private final Supplier<Result_> mapSupplier;
    private final BinaryOperator<Value_> mergeFunction;

    ToSimpleMapBiCollector(BiFunction<? super A, ? super B, ? extends Key_> keyFunction,
            BiFunction<? super A, ? super B, ? extends Value_> valueFunction,
            Supplier<Result_> mapSupplier,
            BinaryOperator<Value_> mergeFunction) {
        super(keyFunction);
        this.keyFunction = keyFunction;
        this.valueFunction = valueFunction;
        this.mapSupplier = mapSupplier;
        this.mergeFunction = mergeFunction;
    }

    @Override
    public @NonNull Supplier<AbstractToMapSlot.State<Key_, Value_, Value_, Result_>> supplier() {
        return () -> AbstractToMapSlot.mergeMapState(mapSupplier, mergeFunction);
    }

    @Override
    public @NonNull Function<AbstractToMapSlot.State<Key_, Value_, Value_, Result_>, Result_> finisher() {
        return state -> state.result();
    }

    @Override
    protected BiConstraintCollectorValueHandle<A, B> newAccumulatedValue(
            AbstractToMapSlot.State<Key_, Value_, Value_, Result_> state) {
        return new Slot(state);
    }

    private final class Slot extends AbstractToMapSlot<Key_, Value_, Value_, Result_>
            implements BiConstraintCollectorValueHandle<A, B> {
        Slot(AbstractToMapSlot.State<Key_, Value_, Value_, Result_> state) {
            super(state);
        }

        @Override
        public void add(A a, B b) {
            addMapped(keyFunction.apply(a, b), valueFunction.apply(a, b));
        }

        @Override
        public void replaceWith(A a, B b) {
            replaceWithMapped(keyFunction.apply(a, b), valueFunction.apply(a, b));
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
        var that = (ToSimpleMapBiCollector<?, ?, ?, ?, ?>) object;
        return Objects.equals(keyFunction, that.keyFunction) && Objects.equals(valueFunction,
                that.valueFunction) && Objects.equals(mapSupplier, that.mapSupplier)
                && Objects.equals(
                        mergeFunction, that.mergeFunction);
    }

    @Override
    public int hashCode() {
        return Objects.hash(keyFunction, valueFunction, mapSupplier, mergeFunction);
    }
}
