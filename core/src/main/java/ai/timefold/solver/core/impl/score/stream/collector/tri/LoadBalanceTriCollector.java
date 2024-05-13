package ai.timefold.solver.core.impl.score.stream.collector.tri;

import java.util.function.Function;
import java.util.function.Supplier;

import ai.timefold.solver.core.api.function.QuadFunction;
import ai.timefold.solver.core.api.function.TriFunction;
import ai.timefold.solver.core.api.score.stream.common.LoadBalance;
import ai.timefold.solver.core.api.score.stream.tri.TriConstraintCollector;
import ai.timefold.solver.core.impl.score.stream.collector.LoadBalanceCalculator;
import ai.timefold.solver.core.impl.score.stream.collector.LoadBalanceImpl;

final class LoadBalanceTriCollector<A, B, C>
        implements TriConstraintCollector<A, B, C, LoadBalanceCalculator, LoadBalance> {

    private final TriFunction<A, B, C, Object> groupKey;

    public LoadBalanceTriCollector(TriFunction<A, B, C, Object> groupKey) {
        this.groupKey = groupKey;
    }

    @Override
    public Supplier<LoadBalanceCalculator> supplier() {
        return LoadBalanceCalculator::new;
    }

    @Override
    public QuadFunction<LoadBalanceCalculator, A, B, C, Runnable> accumulator() {
        return (resultContainer, a, b, c) -> {
            var mapped = groupKey.apply(a, b, c);
            resultContainer.insert(mapped);
            return () -> resultContainer.retract(mapped);
        };
    }

    @Override
    public Function<LoadBalanceCalculator, LoadBalance> finisher() {
        return resultContainer -> new LoadBalanceImpl(resultContainer.result(), resultContainer.getLoads());
    }

}
