package ai.timefold.solver.core.impl.score.stream.collector.uni;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.ToLongFunction;

import ai.timefold.solver.core.api.score.stream.common.LoadBalance;
import ai.timefold.solver.core.api.score.stream.uni.UniConstraintCollector;
import ai.timefold.solver.core.api.score.stream.uni.UniConstraintCollectorAccumulatedValue;
import ai.timefold.solver.core.api.score.stream.uni.UniConstraintCollectorAccumulator;
import ai.timefold.solver.core.impl.score.stream.collector.CollectorUtils;
import ai.timefold.solver.core.impl.score.stream.collector.LoadBalanceImpl;

import org.jspecify.annotations.NonNull;

final class LoadBalanceUniCollector<A, Balanced_>
        implements UniConstraintCollector<A, LoadBalanceImpl<Balanced_>, LoadBalance<Balanced_>> {

    private final Function<A, Balanced_> balancedItemFunction;
    private final ToLongFunction<A> loadFunction;
    private final ToLongFunction<A> initialLoadFunction;

    public LoadBalanceUniCollector(Function<A, Balanced_> balancedItemFunction, ToLongFunction<A> loadFunction,
            ToLongFunction<A> initialLoadFunction) {
        this.balancedItemFunction = balancedItemFunction;
        this.loadFunction = loadFunction;
        this.initialLoadFunction = initialLoadFunction;
    }

    @Override
    public @NonNull Supplier<LoadBalanceImpl<Balanced_>> supplier() {
        return LoadBalanceImpl::new;
    }

    @Override
    public @NonNull BiFunction<LoadBalanceImpl<Balanced_>, A, Runnable> accumulator() {
        return CollectorUtils.fromIncrementalUni(incrementalAccumulator());
    }

    @Override
    public boolean isIncremental() {
        return true;
    }

    @Override
    public @NonNull UniConstraintCollectorAccumulator<LoadBalanceImpl<Balanced_>, A> incrementalAccumulator() {
        return AccumulatedValue::new;
    }

    @Override
    public @NonNull Function<LoadBalanceImpl<Balanced_>, LoadBalance<Balanced_>> finisher() {
        return balanceStatistics -> balanceStatistics;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof LoadBalanceUniCollector<?, ?> that
                && Objects.equals(balancedItemFunction, that.balancedItemFunction)
                && Objects.equals(loadFunction, that.loadFunction)
                && Objects.equals(initialLoadFunction, that.initialLoadFunction);
    }

    @Override
    public int hashCode() {
        return Objects.hash(balancedItemFunction, loadFunction, initialLoadFunction);
    }

    private final class AccumulatedValue implements UniConstraintCollectorAccumulatedValue<A> {
        private final LoadBalanceImpl<Balanced_> container;
        private Runnable undoAction;

        AccumulatedValue(LoadBalanceImpl<Balanced_> container) {
            this.container = container;
        }

        @Override
        public void add(A a) {
            undoAction = container.registerBalanced(
                    balancedItemFunction.apply(a), loadFunction.applyAsLong(a), initialLoadFunction.applyAsLong(a));
        }

        @Override
        public void remove() {
            undoAction.run();
            undoAction = null;
        }
    }
}
