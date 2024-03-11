package ai.timefold.solver.core.impl.score.stream.collector.bi;

import java.util.Map;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.function.Supplier;

import ai.timefold.solver.core.impl.score.stream.collector.MapUndoableActionable;
import ai.timefold.solver.core.impl.util.Pair;

final class ToSimpleMapBiCollector<A, B, Key_, Value_, Result_ extends Map<Key_, Value_>>
        extends
        UndoableActionableBiCollector<A, B, Pair<Key_, Value_>, Result_, MapUndoableActionable<Key_, Value_, Value_, Result_>> {
    private final BiFunction<? super A, ? super B, ? extends Key_> keyFunction;
    private final BiFunction<? super A, ? super B, ? extends Value_> valueFunction;
    private final Supplier<Result_> mapSupplier;
    private final BinaryOperator<Value_> mergeFunction;

    ToSimpleMapBiCollector(BiFunction<? super A, ? super B, ? extends Key_> keyFunction,
            BiFunction<? super A, ? super B, ? extends Value_> valueFunction,
            Supplier<Result_> mapSupplier,
            BinaryOperator<Value_> mergeFunction) {
        super((a, b) -> new Pair<>(keyFunction.apply(a, b), valueFunction.apply(a, b)));
        this.keyFunction = keyFunction;
        this.valueFunction = valueFunction;
        this.mapSupplier = mapSupplier;
        this.mergeFunction = mergeFunction;
    }

    @Override
    public Supplier<MapUndoableActionable<Key_, Value_, Value_, Result_>> supplier() {
        return () -> MapUndoableActionable.mergeMap(mapSupplier, mergeFunction);
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
