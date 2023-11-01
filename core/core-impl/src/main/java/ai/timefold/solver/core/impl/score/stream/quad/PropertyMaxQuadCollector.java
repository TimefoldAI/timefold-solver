package ai.timefold.solver.core.impl.score.stream.quad;

import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;

import ai.timefold.solver.core.api.function.QuadFunction;
import ai.timefold.solver.core.impl.score.stream.MinMaxUndoableActionable;

public final class PropertyMaxQuadCollector<A, B, C, D, Result, Property extends Comparable<? super Property>>
        extends UndoableActionableQuadCollector<A, B, C, D, Result, Result, MinMaxUndoableActionable<Result, Property>> {
    private final Function<? super Result, ? extends Property> propertyMapper;

    public PropertyMaxQuadCollector(QuadFunction<? super A, ? super B, ? super C, ? super D, ? extends Result> mapper,
            Function<? super Result, ? extends Property> propertyMapper) {
        super(mapper);
        this.propertyMapper = propertyMapper;
    }

    @Override
    public Supplier<MinMaxUndoableActionable<Result, Property>> supplier() {
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
        PropertyMaxQuadCollector<?, ?, ?, ?, ?, ?> that = (PropertyMaxQuadCollector<?, ?, ?, ?, ?, ?>) object;
        return Objects.equals(propertyMapper, that.propertyMapper);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), propertyMapper);
    }
}