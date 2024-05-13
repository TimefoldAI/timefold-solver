package ai.timefold.solver.core.impl.score.stream.collector.uni;

import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

import ai.timefold.solver.core.api.score.stream.common.LoadBalance;
import ai.timefold.solver.core.api.score.stream.uni.UniConstraintCollector;
import ai.timefold.solver.core.impl.score.stream.collector.LoadBalanceCalculator;
import ai.timefold.solver.core.impl.score.stream.collector.LoadBalanceImpl;

final class LoadBalanceUniCollector<A>
        implements UniConstraintCollector<A, LoadBalanceCalculator, LoadBalance> {

    private final Function<A, Object> groupKey;

    public LoadBalanceUniCollector(Function<A, Object> groupKey) {
        this.groupKey = groupKey;
    }

    @Override
    public Supplier<LoadBalanceCalculator> supplier() {
        return LoadBalanceCalculator::new;
    }

    @Override
    public BiFunction<LoadBalanceCalculator, A, Runnable> accumulator() {
        return (resultContainer, a) -> {
            var mapped = groupKey.apply(a);
            resultContainer.insert(mapped);
            return () -> resultContainer.retract(mapped);
        };
    }

    @Override
    public Function<LoadBalanceCalculator, LoadBalance> finisher() {
        return resultContainer -> new LoadBalanceImpl(resultContainer.result(), resultContainer.getLoads());
    }

}
