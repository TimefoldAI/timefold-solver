package ai.timefold.solver.core.impl.score.stream.collector.quad;

import java.util.function.Function;
import java.util.function.Supplier;

import ai.timefold.solver.core.api.function.PentaFunction;
import ai.timefold.solver.core.api.function.QuadFunction;
import ai.timefold.solver.core.api.score.stream.common.LoadBalance;
import ai.timefold.solver.core.api.score.stream.quad.QuadConstraintCollector;
import ai.timefold.solver.core.impl.score.stream.collector.LoadBalanceCalculator;
import ai.timefold.solver.core.impl.score.stream.collector.LoadBalanceImpl;

final class LoadBalanceQuadCollector<A, B, C, D>
        implements QuadConstraintCollector<A, B, C, D, LoadBalanceCalculator, LoadBalance> {

    private final QuadFunction<A, B, C, D, Object> groupKey;

    public LoadBalanceQuadCollector(QuadFunction<A, B, C, D, Object> groupKey) {
        this.groupKey = groupKey;
    }

    @Override
    public Supplier<LoadBalanceCalculator> supplier() {
        return LoadBalanceCalculator::new;
    }

    @Override
    public PentaFunction<LoadBalanceCalculator, A, B, C, D, Runnable> accumulator() {
        return (resultContainer, a, b, c, d) -> {
            var mapped = groupKey.apply(a, b, c, d);
            resultContainer.insert(mapped);
            return () -> resultContainer.retract(mapped);
        };
    }

    @Override
    public Function<LoadBalanceCalculator, LoadBalance> finisher() {
        return resultContainer -> new LoadBalanceImpl(resultContainer.result(), resultContainer.getLoads());
    }

}
