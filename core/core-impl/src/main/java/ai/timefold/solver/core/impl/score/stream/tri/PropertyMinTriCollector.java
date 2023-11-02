package ai.timefold.solver.core.impl.score.stream.tri;

import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;

import ai.timefold.solver.core.api.function.TriFunction;
import ai.timefold.solver.core.impl.score.stream.MinMaxUndoableActionable;

public final class PropertyMinTriCollector<A, B, C, Result_, Property_ extends Comparable<? super Property_>>
        extends UndoableActionableTriCollector<A, B, C, Result_, Result_, MinMaxUndoableActionable<Result_, Property_>> {
    private final Function<? super Result_, ? extends Property_> propertyMapper;

    public PropertyMinTriCollector(TriFunction<? super A, ? super B, ? super C, ? extends Result_> mapper,
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
        PropertyMinTriCollector<?, ?, ?, ?, ?> that = (PropertyMinTriCollector<?, ?, ?, ?, ?>) object;
        return Objects.equals(propertyMapper, that.propertyMapper);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), propertyMapper);
    }
}