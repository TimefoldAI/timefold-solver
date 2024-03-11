package ai.timefold.solver.core.impl.score.stream.collector.tri;

import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;

import ai.timefold.solver.core.api.function.QuadFunction;
import ai.timefold.solver.core.api.score.stream.tri.TriConstraintCollector;

final class AndThenTriCollector<A, B, C, ResultContainer_, Intermediate_, Result_>
        implements TriConstraintCollector<A, B, C, ResultContainer_, Result_> {

    private final TriConstraintCollector<A, B, C, ResultContainer_, Intermediate_> delegate;
    private final Function<Intermediate_, Result_> mappingFunction;

    AndThenTriCollector(TriConstraintCollector<A, B, C, ResultContainer_, Intermediate_> delegate,
            Function<Intermediate_, Result_> mappingFunction) {
        this.delegate = Objects.requireNonNull(delegate);
        this.mappingFunction = Objects.requireNonNull(mappingFunction);
    }

    @Override
    public Supplier<ResultContainer_> supplier() {
        return delegate.supplier();
    }

    @Override
    public QuadFunction<ResultContainer_, A, B, C, Runnable> accumulator() {
        return delegate.accumulator();
    }

    @Override
    public Function<ResultContainer_, Result_> finisher() {
        var finisher = delegate.finisher();
        return container -> mappingFunction.apply(finisher.apply(container));
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof AndThenTriCollector<?, ?, ?, ?, ?, ?> other) {
            return Objects.equals(delegate, other.delegate)
                    && Objects.equals(mappingFunction, other.mappingFunction);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(delegate, mappingFunction);
    }
}
