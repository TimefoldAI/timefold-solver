package ai.timefold.solver.core.impl.score.stream.collector.quad;

import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;

import ai.timefold.solver.core.api.function.QuadFunction;
import ai.timefold.solver.core.api.score.stream.quad.QuadConstraintCollectorAccumulatedValue;
import ai.timefold.solver.core.impl.score.stream.collector.AbstractMinMaxSlot;

import org.jspecify.annotations.NonNull;

final class MinPropertyQuadCollector<A, B, C, D, Result_, Property_ extends Comparable<? super Property_>>
        extends
        UndoableActionableQuadCollector<A, B, C, D, Result_, Result_, AbstractMinMaxSlot.State<Result_, Property_>> {
    private final Function<? super Result_, ? extends Property_> propertyMapper;

    MinPropertyQuadCollector(QuadFunction<? super A, ? super B, ? super C, ? super D, ? extends Result_> mapper,
            Function<? super Result_, ? extends Property_> propertyMapper) {
        super(mapper);
        this.propertyMapper = propertyMapper;
    }

    @Override
    public @NonNull Supplier<AbstractMinMaxSlot.State<Result_, Property_>> supplier() {
        return () -> AbstractMinMaxSlot.minState(propertyMapper);
    }

    @Override
    public @NonNull Function<AbstractMinMaxSlot.State<Result_, Property_>, Result_> finisher() {
        return AbstractMinMaxSlot.State::result;
    }

    @Override
    protected QuadConstraintCollectorAccumulatedValue<A, B, C, D> newAccumulatedValue(
            AbstractMinMaxSlot.State<Result_, Property_> state) {
        return new Slot(state);
    }

    private final class Slot extends AbstractMinMaxSlot<Result_, Property_>
            implements QuadConstraintCollectorAccumulatedValue<A, B, C, D> {
        Slot(AbstractMinMaxSlot.State<Result_, Property_> state) {
            super(state);
        }

        @Override
        public void add(A a, B b, C c, D d) {
            addMapped(mapper.apply(a, b, c, d));
        }

        @Override
        public void update(A a, B b, C c, D d) {
            updateMapped(mapper.apply(a, b, c, d));
        }

        @Override
        public void remove() {
            removeMapped();
        }
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
