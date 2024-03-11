package ai.timefold.solver.core.impl.score.stream.collector.tri;

import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;

import ai.timefold.solver.core.api.function.QuadFunction;
import ai.timefold.solver.core.api.function.TriPredicate;
import ai.timefold.solver.core.api.score.stream.tri.TriConstraintCollector;
import ai.timefold.solver.core.impl.util.ConstantLambdaUtils;

final class ConditionalTriCollector<A, B, C, ResultContainer_, Result_>
        implements TriConstraintCollector<A, B, C, ResultContainer_, Result_> {
    private final TriPredicate<A, B, C> predicate;
    private final TriConstraintCollector<A, B, C, ResultContainer_, Result_> delegate;
    private final QuadFunction<ResultContainer_, A, B, C, Runnable> innerAccumulator;

    ConditionalTriCollector(TriPredicate<A, B, C> predicate,
            TriConstraintCollector<A, B, C, ResultContainer_, Result_> delegate) {
        this.predicate = predicate;
        this.delegate = delegate;
        this.innerAccumulator = delegate.accumulator();
    }

    @Override
    public Supplier<ResultContainer_> supplier() {
        return delegate.supplier();
    }

    @Override
    public QuadFunction<ResultContainer_, A, B, C, Runnable> accumulator() {
        return (resultContainer, a, b, c) -> {
            if (predicate.test(a, b, c)) {
                return innerAccumulator.apply(resultContainer, a, b, c);
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
        var that = (ConditionalTriCollector<?, ?, ?, ?, ?>) object;
        return Objects.equals(predicate, that.predicate) && Objects.equals(delegate, that.delegate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(predicate, delegate);
    }
}
