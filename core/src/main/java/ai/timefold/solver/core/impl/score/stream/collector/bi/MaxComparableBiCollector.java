package ai.timefold.solver.core.impl.score.stream.collector.bi;

import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

import ai.timefold.solver.core.api.score.stream.bi.BiConstraintCollectorAccumulatedValue;
import ai.timefold.solver.core.impl.score.stream.collector.AbstractMinMaxSlot;

import org.jspecify.annotations.NonNull;

final class MaxComparableBiCollector<A, B, Result_ extends Comparable<? super Result_>>
        extends
        UndoableActionableBiCollector<A, B, Result_, Result_, AbstractMinMaxSlot.State<Result_, Result_>> {
    MaxComparableBiCollector(BiFunction<? super A, ? super B, ? extends Result_> mapper) {
        super(mapper);
    }

    @Override
    public @NonNull Supplier<AbstractMinMaxSlot.State<Result_, Result_>> supplier() {
        return AbstractMinMaxSlot::maxState;
    }

    @Override
    public @NonNull Function<AbstractMinMaxSlot.State<Result_, Result_>, Result_> finisher() {
        return state -> state.result();
    }

    @Override
    protected BiConstraintCollectorAccumulatedValue<A, B> newAccumulatedValue(
            AbstractMinMaxSlot.State<Result_, Result_> state) {
        return new Slot(state);
    }

    private final class Slot extends AbstractMinMaxSlot<Result_, Result_>
            implements BiConstraintCollectorAccumulatedValue<A, B> {
        Slot(AbstractMinMaxSlot.State<Result_, Result_> state) {
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
}
