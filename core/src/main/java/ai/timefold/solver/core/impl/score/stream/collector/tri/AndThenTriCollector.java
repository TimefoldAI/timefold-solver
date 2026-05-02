package ai.timefold.solver.core.impl.score.stream.collector.tri;

import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;

import ai.timefold.solver.core.api.function.QuadFunction;
import ai.timefold.solver.core.api.score.stream.tri.TriConstraintCollector;
import ai.timefold.solver.core.api.score.stream.tri.TriConstraintCollectorAccumulator;

import org.jspecify.annotations.NonNull;

final class AndThenTriCollector<A, B, C, ResultContainer_, Intermediate_, Result_>
        implements TriConstraintCollector<A, B, C, ResultContainer_, Result_> {

    private final TriConstraintCollector<A, B, C, ResultContainer_, Intermediate_> delegate;
    private final Function<Intermediate_, Result_> mappingFunction;
    private final TriConstraintCollectorAccumulator<ResultContainer_, A, B, C> innerIncremental;

    AndThenTriCollector(TriConstraintCollector<A, B, C, ResultContainer_, Intermediate_> delegate,
            Function<Intermediate_, Result_> mappingFunction) {
        this.delegate = Objects.requireNonNull(delegate);
        this.mappingFunction = Objects.requireNonNull(mappingFunction);
        this.innerIncremental = delegate.isIncremental() ? delegate.incrementalAccumulator()
                : TriCollectorUtils.toIncremental(delegate.accumulator());
    }

    @Override
    public @NonNull Supplier<ResultContainer_> supplier() {
        return delegate.supplier();
    }

    @Override
    public @NonNull QuadFunction<ResultContainer_, A, B, C, Runnable> accumulator() {
        return TriCollectorUtils.fromIncremental(incrementalAccumulator());
    }

    @Override
    public boolean isIncremental() {
        return true;
    }

    @Override
    public @NonNull TriConstraintCollectorAccumulator<ResultContainer_, A, B, C> incrementalAccumulator() {
        return innerIncremental;
    }

    @Override
    public @NonNull Function<ResultContainer_, Result_> finisher() {
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
