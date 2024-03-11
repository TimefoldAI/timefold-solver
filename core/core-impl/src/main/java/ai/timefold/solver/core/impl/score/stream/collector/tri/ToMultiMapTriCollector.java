package ai.timefold.solver.core.impl.score.stream.collector.tri;

import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.IntFunction;
import java.util.function.Supplier;

import ai.timefold.solver.core.api.function.TriFunction;
import ai.timefold.solver.core.impl.score.stream.collector.MapUndoableActionable;
import ai.timefold.solver.core.impl.util.Pair;

final class ToMultiMapTriCollector<A, B, C, Key_, Value_, Set_ extends Set<Value_>, Result_ extends Map<Key_, Set_>>
        extends
        UndoableActionableTriCollector<A, B, C, Pair<Key_, Value_>, Result_, MapUndoableActionable<Key_, Value_, Set_, Result_>> {
    private final TriFunction<? super A, ? super B, ? super C, ? extends Key_> keyFunction;
    private final TriFunction<? super A, ? super B, ? super C, ? extends Value_> valueFunction;
    private final Supplier<Result_> mapSupplier;
    private final IntFunction<Set_> setFunction;

    ToMultiMapTriCollector(TriFunction<? super A, ? super B, ? super C, ? extends Key_> keyFunction,
            TriFunction<? super A, ? super B, ? super C, ? extends Value_> valueFunction,
            Supplier<Result_> mapSupplier,
            IntFunction<Set_> setFunction) {
        super((a, b, c) -> new Pair<>(keyFunction.apply(a, b, c), valueFunction.apply(a, b, c)));
        this.keyFunction = keyFunction;
        this.valueFunction = valueFunction;
        this.mapSupplier = mapSupplier;
        this.setFunction = setFunction;
    }

    @Override
    public Supplier<MapUndoableActionable<Key_, Value_, Set_, Result_>> supplier() {
        return () -> MapUndoableActionable.multiMap(mapSupplier, setFunction);
    }

    // Don't call super equals/hashCode; the groupingFunction is calculated from keyFunction
    // and valueFunction
    @Override
    public boolean equals(Object object) {
        if (this == object)
            return true;
        if (object == null || getClass() != object.getClass())
            return false;
        var that = (ToMultiMapTriCollector<?, ?, ?, ?, ?, ?, ?>) object;
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
