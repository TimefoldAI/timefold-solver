package ai.timefold.solver.core.impl.score.stream.collector.quad;

import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;

import ai.timefold.solver.core.api.function.PentaFunction;
import ai.timefold.solver.core.api.score.stream.quad.QuadConstraintCollector;
import ai.timefold.solver.core.api.score.stream.quad.QuadConstraintCollectorAccumulator;

import org.jspecify.annotations.NonNull;

final class AndThenQuadCollector<A, B, C, D, ResultContainer_, Intermediate_, Result_>
        implements QuadConstraintCollector<A, B, C, D, ResultContainer_, Result_> {

    private final QuadConstraintCollector<A, B, C, D, ResultContainer_, Intermediate_> delegate;
    private final Function<Intermediate_, Result_> mappingFunction;
    private final QuadConstraintCollectorAccumulator<ResultContainer_, A, B, C, D> innerIncremental;

    AndThenQuadCollector(QuadConstraintCollector<A, B, C, D, ResultContainer_, Intermediate_> delegate,
            Function<Intermediate_, Result_> mappingFunction) {
        this.delegate = Objects.requireNonNull(delegate);
        this.mappingFunction = Objects.requireNonNull(mappingFunction);
        this.innerIncremental = delegate.isIncremental() ? delegate.incrementalAccumulator()
                : QuadCollectorUtils.toIncremental(delegate.accumulator());
    }

    @Override
    public @NonNull Supplier<ResultContainer_> supplier() {
        return delegate.supplier();
    }

    @Override
    public @NonNull PentaFunction<ResultContainer_, A, B, C, D, Runnable> accumulator() {
        return QuadCollectorUtils.fromIncremental(incrementalAccumulator());
    }

    @Override
    public boolean isIncremental() {
        return true;
    }

    @Override
    public @NonNull QuadConstraintCollectorAccumulator<ResultContainer_, A, B, C, D> incrementalAccumulator() {
        return innerIncremental;
    }

    @Override
    public @NonNull Function<ResultContainer_, Result_> finisher() {
        var finisher = delegate.finisher();
        return container -> mappingFunction.apply(finisher.apply(container));
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof AndThenQuadCollector<?, ?, ?, ?, ?, ?, ?> other) {
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
