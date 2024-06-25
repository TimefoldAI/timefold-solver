package ai.timefold.solver.core.impl.score.stream.collector.tri;

import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;

import ai.timefold.solver.core.api.function.QuadFunction;
import ai.timefold.solver.core.api.function.ToLongTriFunction;
import ai.timefold.solver.core.api.function.TriFunction;
import ai.timefold.solver.core.api.score.stream.common.LoadBalance;
import ai.timefold.solver.core.api.score.stream.tri.TriConstraintCollector;
import ai.timefold.solver.core.impl.score.stream.collector.LoadBalanceImpl;

final class LoadBalanceTriCollector<A, B, C, Balanced_>
        implements TriConstraintCollector<A, B, C, LoadBalanceImpl<Balanced_>, LoadBalance<Balanced_>> {

    private final TriFunction<A, B, C, Balanced_> balancedItemFunction;
    private final ToLongTriFunction<A, B, C> loadFunction;
    private final ToLongTriFunction<A, B, C> initialLoadFunction;

    public LoadBalanceTriCollector(TriFunction<A, B, C, Balanced_> balancedItemFunction,
            ToLongTriFunction<A, B, C> loadFunction,
            ToLongTriFunction<A, B, C> initialLoadFunction) {
        this.balancedItemFunction = balancedItemFunction;
        this.loadFunction = loadFunction;
        this.initialLoadFunction = initialLoadFunction;
    }

    @Override
    public Supplier<LoadBalanceImpl<Balanced_>> supplier() {
        return LoadBalanceImpl::new;
    }

    @Override
    public QuadFunction<LoadBalanceImpl<Balanced_>, A, B, C, Runnable> accumulator() {
        return (balanceStatistics, a, b, c) -> {
            var balanced = balancedItemFunction.apply(a, b, c);
            var initialLoad = initialLoadFunction.applyAsLong(a, b, c);
            var load = loadFunction.applyAsLong(a, b, c);
            return balanceStatistics.registerBalanced(balanced, load, initialLoad);
        };
    }

    @Override
    public Function<LoadBalanceImpl<Balanced_>, LoadBalance<Balanced_>> finisher() {
        return balanceStatistics -> balanceStatistics;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof LoadBalanceTriCollector<?, ?, ?, ?> that
                && Objects.equals(balancedItemFunction, that.balancedItemFunction)
                && Objects.equals(loadFunction, that.loadFunction)
                && Objects.equals(initialLoadFunction, that.initialLoadFunction);
    }

    @Override
    public int hashCode() {
        return Objects.hash(balancedItemFunction, loadFunction, initialLoadFunction);
    }
}
