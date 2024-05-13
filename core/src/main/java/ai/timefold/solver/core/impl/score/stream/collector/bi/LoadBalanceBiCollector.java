package ai.timefold.solver.core.impl.score.stream.collector.bi;

import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

import ai.timefold.solver.core.api.function.TriFunction;
import ai.timefold.solver.core.api.score.stream.bi.BiConstraintCollector;
import ai.timefold.solver.core.api.score.stream.common.LoadBalance;
import ai.timefold.solver.core.impl.score.stream.collector.LoadBalanceCalculator;
import ai.timefold.solver.core.impl.score.stream.collector.LoadBalanceImpl;

final class LoadBalanceBiCollector<A, B>
        implements BiConstraintCollector<A, B, LoadBalanceCalculator, LoadBalance> {

    private final BiFunction<A, B, Object> groupKey;

    public LoadBalanceBiCollector(BiFunction<A, B, Object> groupKey) {
        this.groupKey = groupKey;
    }

    @Override
    public Supplier<LoadBalanceCalculator> supplier() {
        return LoadBalanceCalculator::new;
    }

    @Override
    public TriFunction<LoadBalanceCalculator, A, B, Runnable> accumulator() {
        return (resultContainer, a, b) -> {
            var mapped = groupKey.apply(a, b);
            resultContainer.insert(mapped);
            return () -> resultContainer.retract(mapped);
        };
    }

    @Override
    public Function<LoadBalanceCalculator, LoadBalance> finisher() {
        return resultContainer -> new LoadBalanceImpl(resultContainer.result(), resultContainer.getLoads());
    }

}
