package ai.timefold.solver.core.impl.score.stream.collector.uni;

import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;

import ai.timefold.solver.core.impl.score.stream.collector.MinMaxUndoableActionable;

final class MaxPropertyUniCollector<A, Result_, Property_ extends Comparable<? super Property_>>
        extends UndoableActionableUniCollector<A, Result_, Result_, MinMaxUndoableActionable<Result_, Property_>> {
    private final Function<? super Result_, ? extends Property_> propertyMapper;

    MaxPropertyUniCollector(Function<? super A, ? extends Result_> mapper,
            Function<? super Result_, ? extends Property_> propertyMapper) {
        super(mapper);
        this.propertyMapper = propertyMapper;
    }

    @Override
    public Supplier<MinMaxUndoableActionable<Result_, Property_>> supplier() {
        return () -> MinMaxUndoableActionable.maxCalculator(propertyMapper);
    }

    @Override
    public boolean equals(Object object) {
        if (this == object)
            return true;
        if (object == null || getClass() != object.getClass())
            return false;
        if (!super.equals(object))
            return false;
        MaxPropertyUniCollector<?, ?, ?> that = (MaxPropertyUniCollector<?, ?, ?>) object;
        return Objects.equals(propertyMapper, that.propertyMapper);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), propertyMapper);
    }
}