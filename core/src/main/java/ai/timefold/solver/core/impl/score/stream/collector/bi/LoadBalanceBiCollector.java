package ai.timefold.solver.core.impl.score.stream.collector.bi;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.ToLongBiFunction;

import ai.timefold.solver.core.api.function.TriFunction;
import ai.timefold.solver.core.api.score.stream.bi.BiConstraintCollector;
import ai.timefold.solver.core.api.score.stream.common.LoadBalance;
import ai.timefold.solver.core.impl.score.stream.collector.LoadBalanceImpl;

final class LoadBalanceBiCollector<A, B, Balanced_>
        implements BiConstraintCollector<A, B, LoadBalanceImpl<Balanced_>, LoadBalance<Balanced_>> {

    private final BiFunction<A, B, Balanced_> balancedItemFunction;
    private final ToLongBiFunction<A, B> loadFunction;
    private final ToLongBiFunction<A, B> initialLoadFunction;

    public LoadBalanceBiCollector(BiFunction<A, B, Balanced_> balancedItemFunction, ToLongBiFunction<A, B> loadFunction,
            ToLongBiFunction<A, B> initialLoadFunction) {
        this.balancedItemFunction = balancedItemFunction;
        this.loadFunction = loadFunction;
        this.initialLoadFunction = initialLoadFunction;
    }

    @Override
    public Supplier<LoadBalanceImpl<Balanced_>> supplier() {
        return LoadBalanceImpl::new;
    }

    @Override
    public TriFunction<LoadBalanceImpl<Balanced_>, A, B, Runnable> accumulator() {
        return (balanceStatistics, a, b) -> {
            var balanced = balancedItemFunction.apply(a, b);
            var initialLoad = initialLoadFunction.applyAsLong(a, b);
            var load = loadFunction.applyAsLong(a, b);
            return balanceStatistics.registerBalanced(balanced, load, initialLoad);
        };
    }

    @Override
    public Function<LoadBalanceImpl<Balanced_>, LoadBalance<Balanced_>> finisher() {
        return balanceStatistics -> balanceStatistics;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof LoadBalanceBiCollector<?, ?, ?> that
                && Objects.equals(balancedItemFunction, that.balancedItemFunction)
                && Objects.equals(loadFunction, that.loadFunction)
                && Objects.equals(initialLoadFunction, that.initialLoadFunction);
    }

    @Override
    public int hashCode() {
        return Objects.hash(balancedItemFunction, loadFunction, initialLoadFunction);
    }
}
