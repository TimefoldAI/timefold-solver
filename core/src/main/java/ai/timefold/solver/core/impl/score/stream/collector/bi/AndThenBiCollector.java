package ai.timefold.solver.core.impl.score.stream.collector.bi;

import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;

import ai.timefold.solver.core.api.function.TriFunction;
import ai.timefold.solver.core.api.score.stream.bi.BiConstraintCollector;

final class AndThenBiCollector<A, B, ResultContainer_, Intermediate_, Result_>
        implements BiConstraintCollector<A, B, ResultContainer_, Result_> {

    private final BiConstraintCollector<A, B, ResultContainer_, Intermediate_> delegate;
    private final Function<Intermediate_, Result_> mappingFunction;

    AndThenBiCollector(BiConstraintCollector<A, B, ResultContainer_, Intermediate_> delegate,
            Function<Intermediate_, Result_> mappingFunction) {
        this.delegate = Objects.requireNonNull(delegate);
        this.mappingFunction = Objects.requireNonNull(mappingFunction);
    }

    @Override
    public Supplier<ResultContainer_> supplier() {
        return delegate.supplier();
    }

    @Override
    public TriFunction<ResultContainer_, A, B, Runnable> accumulator() {
        return delegate.accumulator();
    }

    @Override
    public Function<ResultContainer_, Result_> finisher() {
        var finisher = delegate.finisher();
        return container -> mappingFunction.apply(finisher.apply(container));
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof AndThenBiCollector<?, ?, ?, ?, ?> other) {
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
