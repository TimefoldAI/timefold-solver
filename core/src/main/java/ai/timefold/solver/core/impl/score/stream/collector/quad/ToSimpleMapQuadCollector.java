package ai.timefold.solver.core.impl.score.stream.collector.quad;

import java.util.Map;
import java.util.Objects;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;

import ai.timefold.solver.core.api.function.QuadFunction;
import ai.timefold.solver.core.api.score.stream.quad.QuadConstraintCollectorValueHandle;
import ai.timefold.solver.core.impl.score.stream.collector.AbstractToMapSlot;

import org.jspecify.annotations.NonNull;

final class ToSimpleMapQuadCollector<A, B, C, D, Key_, Value_, Result_ extends Map<Key_, Value_>>
        extends
        UndoableActionableQuadCollector<A, B, C, D, Key_, Result_, AbstractToMapSlot.State<Key_, Value_, Value_, Result_>> {
    private final QuadFunction<? super A, ? super B, ? super C, ? super D, ? extends Key_> keyFunction;
    private final QuadFunction<? super A, ? super B, ? super C, ? super D, ? extends Value_> valueFunction;
    private final Supplier<Result_> mapSupplier;
    private final BinaryOperator<Value_> mergeFunction;

    ToSimpleMapQuadCollector(QuadFunction<? super A, ? super B, ? super C, ? super D, ? extends Key_> keyFunction,
            QuadFunction<? super A, ? super B, ? super C, ? super D, ? extends Value_> valueFunction,
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
    protected QuadConstraintCollectorValueHandle<A, B, C, D> newAccumulatedValue(
            AbstractToMapSlot.State<Key_, Value_, Value_, Result_> state) {
        return new Slot(state);
    }

    private final class Slot extends AbstractToMapSlot<Key_, Value_, Value_, Result_>
            implements QuadConstraintCollectorValueHandle<A, B, C, D> {
        Slot(AbstractToMapSlot.State<Key_, Value_, Value_, Result_> state) {
            super(state);
        }

        @Override
        public void add(A a, B b, C c, D d) {
            addMapped(keyFunction.apply(a, b, c, d), valueFunction.apply(a, b, c, d));
        }

        @Override
        public void update(A a, B b, C c, D d) {
            updateMapped(keyFunction.apply(a, b, c, d), valueFunction.apply(a, b, c, d));
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
        var that = (ToSimpleMapQuadCollector<?, ?, ?, ?, ?, ?, ?>) object;
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
