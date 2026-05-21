package ai.timefold.solver.core.impl.score.stream.collector.uni;

import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;

import ai.timefold.solver.core.api.score.stream.uni.UniConstraintCollectorValueHandle;
import ai.timefold.solver.core.impl.score.stream.collector.AbstractMinMaxSlot;

import org.jspecify.annotations.NonNull;

final class MaxPropertyUniCollector<A, Result_, Property_ extends Comparable<? super Property_>>
        extends AbstractReferenceBasedUniCollector<A, Result_, Result_, AbstractMinMaxSlot.State<Result_, Property_>> {
    private final Function<? super Result_, ? extends Property_> propertyMapper;

    MaxPropertyUniCollector(Function<? super A, ? extends Result_> mapper,
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
        return AbstractMinMaxSlot.State::result;
    }

    @Override
    protected UniConstraintCollectorValueHandle<A>
            newAccumulatedValue(AbstractMinMaxSlot.State<Result_, Property_> state) {
        return new Slot(state);
    }

    private final class Slot extends AbstractMinMaxSlot<Result_, Property_>
            implements UniConstraintCollectorValueHandle<A> {
        Slot(AbstractMinMaxSlot.State<Result_, Property_> state) {
            super(state);
        }

        @Override
        public void add(A a) {
            addMapped(mapper.apply(a));
        }

        @Override
        public void replaceWith(A a) {
            replaceWithMapped(mapper.apply(a));
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
        MaxPropertyUniCollector<?, ?, ?> that = (MaxPropertyUniCollector<?, ?, ?>) object;
        return Objects.equals(propertyMapper, that.propertyMapper);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), propertyMapper);
    }
}
