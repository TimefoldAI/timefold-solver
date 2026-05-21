package ai.timefold.solver.core.impl.score.stream.collector.bi;

import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

import ai.timefold.solver.core.api.score.stream.bi.BiConstraintCollectorValueHandle;
import ai.timefold.solver.core.impl.score.stream.collector.AbstractMinMaxSlot;

import org.jspecify.annotations.NonNull;

final class MinComparableBiCollector<A, B, Result_ extends Comparable<? super Result_>>
        extends AbstractReferenceBasedBiCollector<A, B, Result_, Result_, AbstractMinMaxSlot.State<Result_, Result_>> {
    MinComparableBiCollector(BiFunction<? super A, ? super B, ? extends Result_> mapper) {
        super(mapper);
    }

    @Override
    public @NonNull Supplier<AbstractMinMaxSlot.State<Result_, Result_>> supplier() {
        return AbstractMinMaxSlot::minState;
    }

    @Override
    public @NonNull Function<AbstractMinMaxSlot.State<Result_, Result_>, Result_> finisher() {
        return AbstractMinMaxSlot.State::result;
    }

    @Override
    protected BiConstraintCollectorValueHandle<A, B> newAccumulatedValue(
            AbstractMinMaxSlot.State<Result_, Result_> state) {
        return new Slot(state);
    }

    private final class Slot extends AbstractMinMaxSlot<Result_, Result_>
            implements BiConstraintCollectorValueHandle<A, B> {
        Slot(AbstractMinMaxSlot.State<Result_, Result_> state) {
            super(state);
        }

        @Override
        public void add(A a, B b) {
            addMapped(mapper.apply(a, b));
        }

        @Override
        public void replaceWith(A a, B b) {
            replaceWithMapped(mapper.apply(a, b));
        }

        @Override
        public void remove() {
            removeMapped();
        }
    }
}
