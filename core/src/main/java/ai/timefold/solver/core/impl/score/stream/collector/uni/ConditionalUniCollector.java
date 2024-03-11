package ai.timefold.solver.core.impl.score.stream.collector.uni;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import ai.timefold.solver.core.api.score.stream.uni.UniConstraintCollector;
import ai.timefold.solver.core.impl.util.ConstantLambdaUtils;

final class ConditionalUniCollector<A, ResultContainer_, Result_>
        implements UniConstraintCollector<A, ResultContainer_, Result_> {
    private final Predicate<A> predicate;
    private final UniConstraintCollector<A, ResultContainer_, Result_> delegate;
    private final BiFunction<ResultContainer_, A, Runnable> innerAccumulator;

    ConditionalUniCollector(Predicate<A> predicate, UniConstraintCollector<A, ResultContainer_, Result_> delegate) {
        this.predicate = predicate;
        this.delegate = delegate;
        this.innerAccumulator = delegate.accumulator();
    }

    @Override
    public Supplier<ResultContainer_> supplier() {
        return delegate.supplier();
    }

    @Override
    public BiFunction<ResultContainer_, A, Runnable> accumulator() {
        return (resultContainer, a) -> {
            if (predicate.test(a)) {
                return innerAccumulator.apply(resultContainer, a);
            } else {
                return ConstantLambdaUtils.noop();
            }
        };
    }

    @Override
    public Function<ResultContainer_, Result_> finisher() {
        return delegate.finisher();
    }

    @Override
    public boolean equals(Object object) {
        if (this == object)
            return true;
        if (object == null || getClass() != object.getClass())
            return false;
        var that = (ConditionalUniCollector<?, ?, ?>) object;
        return Objects.equals(predicate, that.predicate) && Objects.equals(delegate, that.delegate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(predicate, delegate);
    }
}
