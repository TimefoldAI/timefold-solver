package ai.timefold.solver.core.impl.score.stream.collector.uni;

import java.util.function.Function;
import java.util.function.Supplier;

import ai.timefold.solver.core.api.score.stream.uni.UniConstraintCollectorAccumulatedValue;
import ai.timefold.solver.core.impl.score.stream.collector.AbstractMinMaxSlot;

import org.jspecify.annotations.NonNull;

final class MinComparableUniCollector<A, Result_ extends Comparable<? super Result_>>
        extends UndoableActionableUniCollector<A, Result_, Result_, AbstractMinMaxSlot.State<Result_, Result_>> {
    MinComparableUniCollector(Function<? super A, ? extends Result_> mapper) {
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
    protected UniConstraintCollectorAccumulatedValue<A>
            newAccumulatedValue(AbstractMinMaxSlot.State<Result_, Result_> state) {
        return new Slot(state);
    }

    private final class Slot extends AbstractMinMaxSlot<Result_, Result_>
            implements UniConstraintCollectorAccumulatedValue<A> {
        Slot(AbstractMinMaxSlot.State<Result_, Result_> state) {
            super(state);
        }

        @Override
        public void add(A a) {
            addMapped(mapper.apply(a));
        }

        @Override
        public void update(A a) {
            updateMapped(mapper.apply(a));
        }

        @Override
        public void remove() {
            removeMapped();
        }
    }
}
