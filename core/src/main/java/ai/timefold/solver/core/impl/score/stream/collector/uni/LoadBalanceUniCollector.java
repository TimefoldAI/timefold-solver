package ai.timefold.solver.core.impl.score.stream.collector.uni;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.ToLongFunction;

import ai.timefold.solver.core.api.score.stream.common.LoadBalance;
import ai.timefold.solver.core.api.score.stream.uni.UniConstraintCollector;
import ai.timefold.solver.core.impl.score.stream.collector.LoadBalanceImpl;

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
    public Supplier<LoadBalanceImpl<Balanced_>> supplier() {
        return LoadBalanceImpl::new;
    }

    @Override
    public BiFunction<LoadBalanceImpl<Balanced_>, A, Runnable> accumulator() {
        return (balanceStatistics, a) -> {
            var balanced = balancedItemFunction.apply(a);
            var initialLoad = initialLoadFunction.applyAsLong(a);
            var load = loadFunction.applyAsLong(a);
            return balanceStatistics.registerBalanced(balanced, load, initialLoad);
        };
    }

    @Override
    public Function<LoadBalanceImpl<Balanced_>, LoadBalance<Balanced_>> finisher() {
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
}
