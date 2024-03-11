package ai.timefold.solver.core.impl.score.stream.collector.quad;

import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;

import ai.timefold.solver.core.api.function.PentaFunction;
import ai.timefold.solver.core.api.function.QuadPredicate;
import ai.timefold.solver.core.api.score.stream.quad.QuadConstraintCollector;
import ai.timefold.solver.core.impl.util.ConstantLambdaUtils;

final class ConditionalQuadCollector<A, B, C, D, ResultContainer_, Result_>
        implements QuadConstraintCollector<A, B, C, D, ResultContainer_, Result_> {
    private final QuadPredicate<A, B, C, D> predicate;
    private final QuadConstraintCollector<A, B, C, D, ResultContainer_, Result_> delegate;
    private final PentaFunction<ResultContainer_, A, B, C, D, Runnable> innerAccumulator;

    ConditionalQuadCollector(QuadPredicate<A, B, C, D> predicate,
            QuadConstraintCollector<A, B, C, D, ResultContainer_, Result_> delegate) {
        this.predicate = predicate;
        this.delegate = delegate;
        this.innerAccumulator = delegate.accumulator();
    }

    @Override
    public Supplier<ResultContainer_> supplier() {
        return delegate.supplier();
    }

    @Override
    public PentaFunction<ResultContainer_, A, B, C, D, Runnable> accumulator() {
        return (resultContainer, a, b, c, d) -> {
            if (predicate.test(a, b, c, d)) {
                return innerAccumulator.apply(resultContainer, a, b, c, d);
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
        var that = (ConditionalQuadCollector<?, ?, ?, ?, ?, ?>) object;
        return Objects.equals(predicate, that.predicate) && Objects.equals(delegate, that.delegate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(predicate, delegate);
    }
}
