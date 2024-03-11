package ai.timefold.solver.core.impl.score.stream.collector.quad;

import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;

import ai.timefold.solver.core.api.function.QuadFunction;
import ai.timefold.solver.core.impl.score.stream.collector.MinMaxUndoableActionable;

final class MinPropertyQuadCollector<A, B, C, D, Result_, Property_ extends Comparable<? super Property_>>
        extends UndoableActionableQuadCollector<A, B, C, D, Result_, Result_, MinMaxUndoableActionable<Result_, Property_>> {
    private final Function<? super Result_, ? extends Property_> propertyMapper;

    MinPropertyQuadCollector(QuadFunction<? super A, ? super B, ? super C, ? super D, ? extends Result_> mapper,
            Function<? super Result_, ? extends Property_> propertyMapper) {
        super(mapper);
        this.propertyMapper = propertyMapper;
    }

    @Override
    public Supplier<MinMaxUndoableActionable<Result_, Property_>> supplier() {
        return () -> MinMaxUndoableActionable.minCalculator(propertyMapper);
    }

    @Override
    public boolean equals(Object object) {
        if (this == object)
            return true;
        if (object == null || getClass() != object.getClass())
            return false;
        if (!super.equals(object))
            return false;
        MinPropertyQuadCollector<?, ?, ?, ?, ?, ?> that = (MinPropertyQuadCollector<?, ?, ?, ?, ?, ?>) object;
        return Objects.equals(propertyMapper, that.propertyMapper);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), propertyMapper);
    }
}