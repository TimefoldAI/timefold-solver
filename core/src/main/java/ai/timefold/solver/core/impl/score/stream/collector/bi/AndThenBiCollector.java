package ai.timefold.solver.core.impl.score.stream.collector.bi;

import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;

import ai.timefold.solver.core.api.function.TriFunction;
import ai.timefold.solver.core.api.score.stream.bi.BiConstraintCollector;
import ai.timefold.solver.core.api.score.stream.bi.BiConstraintCollectorAccumulator;

import org.jspecify.annotations.NonNull;

final class AndThenBiCollector<A, B, ResultContainer_, Intermediate_, Result_>
        implements BiConstraintCollector<A, B, ResultContainer_, Result_> {

    private final BiConstraintCollector<A, B, ResultContainer_, Intermediate_> delegate;
    private final Function<Intermediate_, Result_> mappingFunction;
    private final BiConstraintCollectorAccumulator<ResultContainer_, A, B> innerIncremental;

    AndThenBiCollector(BiConstraintCollector<A, B, ResultContainer_, Intermediate_> delegate,
            Function<Intermediate_, Result_> mappingFunction) {
        this.delegate = Objects.requireNonNull(delegate);
        this.mappingFunction = Objects.requireNonNull(mappingFunction);
        this.innerIncremental = delegate.isIncremental() ? delegate.incrementalAccumulator()
                : BiCollectorUtils.toIncremental(delegate.accumulator());
    }

    @Override
    public @NonNull Supplier<ResultContainer_> supplier() {
        return delegate.supplier();
    }

    @Override
    public @NonNull TriFunction<ResultContainer_, A, B, Runnable> accumulator() {
        return BiCollectorUtils.fromIncremental(incrementalAccumulator());
    }

    @Override
    public boolean isIncremental() {
        return true;
    }

    @Override
    public @NonNull BiConstraintCollectorAccumulator<ResultContainer_, A, B> incrementalAccumulator() {
        return innerIncremental;
    }

    @Override
    public @NonNull Function<ResultContainer_, Result_> finisher() {
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
