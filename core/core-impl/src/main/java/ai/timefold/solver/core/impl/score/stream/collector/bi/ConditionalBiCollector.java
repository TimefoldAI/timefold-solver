package ai.timefold.solver.core.impl.score.stream.collector.bi;

import java.util.Objects;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Supplier;

import ai.timefold.solver.core.api.function.TriFunction;
import ai.timefold.solver.core.api.score.stream.bi.BiConstraintCollector;
import ai.timefold.solver.core.impl.util.ConstantLambdaUtils;

final class ConditionalBiCollector<A, B, ResultContainer_, Result_>
        implements BiConstraintCollector<A, B, ResultContainer_, Result_> {
    private final BiPredicate<A, B> predicate;
    private final BiConstraintCollector<A, B, ResultContainer_, Result_> delegate;
    private final TriFunction<ResultContainer_, A, B, Runnable> innerAccumulator;

    ConditionalBiCollector(BiPredicate<A, B> predicate,
            BiConstraintCollector<A, B, ResultContainer_, Result_> delegate) {
        this.predicate = predicate;
        this.delegate = delegate;
        this.innerAccumulator = delegate.accumulator();
    }

    @Override
    public Supplier<ResultContainer_> supplier() {
        return delegate.supplier();
    }

    @Override
    public TriFunction<ResultContainer_, A, B, Runnable> accumulator() {
        return (resultContainer, a, b) -> {
            if (predicate.test(a, b)) {
                return innerAccumulator.apply(resultContainer, a, b);
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
        var that = (ConditionalBiCollector<?, ?, ?, ?>) object;
        return Objects.equals(predicate, that.predicate) && Objects.equals(delegate, that.delegate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(predicate, delegate);
    }
}
