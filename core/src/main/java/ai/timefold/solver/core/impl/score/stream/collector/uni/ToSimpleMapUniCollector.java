package ai.timefold.solver.core.impl.score.stream.collector.uni;

import java.util.Map;
import java.util.Objects;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;

import ai.timefold.solver.core.impl.score.stream.collector.MapUndoableActionable;
import ai.timefold.solver.core.impl.util.Pair;

final class ToSimpleMapUniCollector<A, Key_, Value_, Result_ extends Map<Key_, Value_>>
        extends
        UndoableActionableUniCollector<A, Pair<Key_, Value_>, Result_, MapUndoableActionable<Key_, Value_, Value_, Result_>> {
    private final Function<? super A, ? extends Key_> keyFunction;
    private final Function<? super A, ? extends Value_> valueFunction;
    private final Supplier<Result_> mapSupplier;
    private final BinaryOperator<Value_> mergeFunction;

    ToSimpleMapUniCollector(Function<? super A, ? extends Key_> keyFunction,
            Function<? super A, ? extends Value_> valueFunction,
            Supplier<Result_> mapSupplier,
            BinaryOperator<Value_> mergeFunction) {
        super(a -> new Pair<>(keyFunction.apply(a), valueFunction.apply(a)));
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
