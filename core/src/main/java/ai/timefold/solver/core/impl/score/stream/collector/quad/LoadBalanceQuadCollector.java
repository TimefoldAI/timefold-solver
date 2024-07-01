package ai.timefold.solver.core.impl.score.stream.collector.quad;

import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;

import ai.timefold.solver.core.api.function.PentaFunction;
import ai.timefold.solver.core.api.function.QuadFunction;
import ai.timefold.solver.core.api.function.ToLongQuadFunction;
import ai.timefold.solver.core.api.score.stream.common.LoadBalance;
import ai.timefold.solver.core.api.score.stream.quad.QuadConstraintCollector;
import ai.timefold.solver.core.impl.score.stream.collector.LoadBalanceImpl;

final class LoadBalanceQuadCollector<A, B, C, D, Balanced_>
        implements QuadConstraintCollector<A, B, C, D, LoadBalanceImpl<Balanced_>, LoadBalance<Balanced_>> {

    private final QuadFunction<A, B, C, D, Balanced_> balancedItemFunction;
    private final ToLongQuadFunction<A, B, C, D> loadFunction;
    private final ToLongQuadFunction<A, B, C, D> initialLoadFunction;

    public LoadBalanceQuadCollector(QuadFunction<A, B, C, D, Balanced_> balancedItemFunction,
            ToLongQuadFunction<A, B, C, D> loadFunction, ToLongQuadFunction<A, B, C, D> initialLoadFunction) {
        this.balancedItemFunction = balancedItemFunction;
        this.loadFunction = loadFunction;
        this.initialLoadFunction = initialLoadFunction;
    }

    @Override
    public Supplier<LoadBalanceImpl<Balanced_>> supplier() {
        return LoadBalanceImpl::new;
    }

    @Override
    public PentaFunction<LoadBalanceImpl<Balanced_>, A, B, C, D, Runnable> accumulator() {
        return (balanceStatistics, a, b, c, d) -> {
            var balanced = balancedItemFunction.apply(a, b, c, d);
            var initialLoad = initialLoadFunction.applyAsLong(a, b, c, d);
            var load = loadFunction.applyAsLong(a, b, c, d);
            return balanceStatistics.registerBalanced(balanced, load, initialLoad);
        };
    }

    @Override
    public Function<LoadBalanceImpl<Balanced_>, LoadBalance<Balanced_>> finisher() {
        return balanceStatistics -> balanceStatistics;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof LoadBalanceQuadCollector<?, ?, ?, ?, ?> that
                && Objects.equals(balancedItemFunction, that.balancedItemFunction)
                && Objects.equals(loadFunction, that.loadFunction)
                && Objects.equals(initialLoadFunction, that.initialLoadFunction);
    }

    @Override
    public int hashCode() {
        return Objects.hash(balancedItemFunction, loadFunction, initialLoadFunction);
    }
}
