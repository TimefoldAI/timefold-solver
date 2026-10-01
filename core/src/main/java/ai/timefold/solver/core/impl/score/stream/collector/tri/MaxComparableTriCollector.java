package ai.timefold.solver.core.impl.score.stream.collector.tri;

import java.util.function.Function;
import java.util.function.Supplier;

import ai.timefold.solver.core.api.function.TriFunction;
import ai.timefold.solver.core.api.score.stream.tri.TriConstraintCollectorValueHandle;
import ai.timefold.solver.core.impl.score.stream.collector.AbstractMinMaxSlot;

import org.jspecify.annotations.NonNull;

final class MaxComparableTriCollector<A, B, C, Result_ extends Comparable<? super Result_>>
        extends AbstractReferenceBasedTriCollector<A, B, C, Result_, Result_, AbstractMinMaxSlot.State<Result_, Result_>> {
    MaxComparableTriCollector(TriFunction<? super A, ? super B, ? super C, ? extends Result_> mapper) {
        super(mapper);
    }

    @Override
    public @NonNull Supplier<AbstractMinMaxSlot.State<Result_, Result_>> supplier() {
        return AbstractMinMaxSlot::maxState;
    }

    @Override
    public @NonNull Function<AbstractMinMaxSlot.State<Result_, Result_>, Result_> finisher() {
        return AbstractMinMaxSlot.State::result;
    }

    @Override
    protected TriConstraintCollectorValueHandle<A, B, C> newAccumulatedValue(
            AbstractMinMaxSlot.State<Result_, Result_> state) {
        return new Slot(state);
    }

    private final class Slot extends AbstractMinMaxSlot<Result_, Result_>
            implements TriConstraintCollectorValueHandle<A, B, C> {
        Slot(AbstractMinMaxSlot.State<Result_, Result_> state) {
            super(state);
        }

        @Override
        public void add(A a, B b, C c) {
            addMapped(mapper.apply(a, b, c));
        }

        @Override
        public void replaceWith(A a, B b, C c) {
            replaceWithMapped(mapper.apply(a, b, c));
        }

        @Override
        public void remove() {
            removeMapped();
        }
    }
}
