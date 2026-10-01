package ai.timefold.solver.core.impl.score.stream.collector.uni;

import java.util.Map;
import java.util.Objects;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;

import ai.timefold.solver.core.api.score.stream.uni.UniConstraintCollectorValueHandle;
import ai.timefold.solver.core.impl.score.stream.collector.AbstractToMapSlot;

import org.jspecify.annotations.NonNull;

final class ToSimpleMapUniCollector<A, Key_, Value_, Result_ extends Map<Key_, Value_>>
        extends AbstractReferenceBasedUniCollector<A, Key_, Result_, AbstractToMapSlot.State<Key_, Value_, Value_, Result_>> {
    private final Function<? super A, ? extends Key_> keyFunction;
    private final Function<? super A, ? extends Value_> valueFunction;
    private final Supplier<Result_> mapSupplier;
    private final BinaryOperator<Value_> mergeFunction;

    ToSimpleMapUniCollector(Function<? super A, ? extends Key_> keyFunction,
            Function<? super A, ? extends Value_> valueFunction,
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
        return AbstractToMapSlot.State::result;
    }

    @Override
    protected UniConstraintCollectorValueHandle<A>
            newAccumulatedValue(AbstractToMapSlot.State<Key_, Value_, Value_, Result_> state) {
        return new Slot(state);
    }

    private final class Slot extends AbstractToMapSlot<Key_, Value_, Value_, Result_>
            implements UniConstraintCollectorValueHandle<A> {
        Slot(AbstractToMapSlot.State<Key_, Value_, Value_, Result_> state) {
            super(state);
        }

        @Override
        public void add(A a) {
            addMapped(keyFunction.apply(a), valueFunction.apply(a));
        }

        @Override
        public void replaceWith(A a) {
            replaceWithMapped(keyFunction.apply(a), valueFunction.apply(a));
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
        var that = (ToSimpleMapUniCollector<?, ?, ?, ?>) object;
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
