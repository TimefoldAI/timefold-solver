package ai.timefold.solver.core.impl.score.stream.collector.bi;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

import ai.timefold.solver.core.api.score.stream.bi.BiConstraintCollectorAccumulatedValue;
import ai.timefold.solver.core.impl.score.stream.collector.AbstractMinMaxSlot;

import org.jspecify.annotations.NonNull;

final class MaxPropertyBiCollector<A, B, Result_, Property_ extends Comparable<? super Property_>>
        extends
        UndoableActionableBiCollector<A, B, Result_, Result_, AbstractMinMaxSlot.State<Result_, Property_>> {
    private final Function<? super Result_, ? extends Property_> propertyMapper;

    MaxPropertyBiCollector(BiFunction<? super A, ? super B, ? extends Result_> mapper,
            Function<? super Result_, ? extends Property_> propertyMapper) {
        super(mapper);
        this.propertyMapper = propertyMapper;
    }

    @Override
    public @NonNull Supplier<AbstractMinMaxSlot.State<Result_, Property_>> supplier() {
        return () -> AbstractMinMaxSlot.maxState(propertyMapper);
    }

    @Override
    public @NonNull Function<AbstractMinMaxSlot.State<Result_, Property_>, Result_> finisher() {
        return state -> state.result();
    }

    @Override
    protected BiConstraintCollectorAccumulatedValue<A, B> newAccumulatedValue(
            AbstractMinMaxSlot.State<Result_, Property_> state) {
        return new Slot(state);
    }

    private final class Slot extends AbstractMinMaxSlot<Result_, Property_>
            implements BiConstraintCollectorAccumulatedValue<A, B> {
        Slot(AbstractMinMaxSlot.State<Result_, Property_> state) {
            super(state);
        }

        @Override
        public void add(A a, B b) {
            addMapped(mapper.apply(a, b));
        }

        @Override
        public void update(A a, B b) {
            updateMapped(mapper.apply(a, b));
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
        MaxPropertyBiCollector<?, ?, ?, ?> that = (MaxPropertyBiCollector<?, ?, ?, ?>) object;
        return Objects.equals(propertyMapper, that.propertyMapper);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), propertyMapper);
    }
}
