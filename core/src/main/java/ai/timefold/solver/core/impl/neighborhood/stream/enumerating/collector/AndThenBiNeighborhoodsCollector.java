package ai.timefold.solver.core.impl.neighborhood.stream.enumerating.collector;

import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;

import ai.timefold.solver.core.preview.api.neighborhood.stream.enumerating.collector.BiNeighborhoodsCollector;
import ai.timefold.solver.core.preview.api.neighborhood.stream.enumerating.collector.BiNeighborhoodsCollectorAccumulator;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public final class AndThenBiNeighborhoodsCollector<Solution_, A, B, ResultContainer_, Intermediate_, Result_>
        implements BiNeighborhoodsCollector<Solution_, A, B, ResultContainer_, Result_> {

    private final BiNeighborhoodsCollector<Solution_, A, B, ResultContainer_, Intermediate_> delegate;
    private final Function<@Nullable Intermediate_, @Nullable Result_> mappingFunction;

    public AndThenBiNeighborhoodsCollector(
            BiNeighborhoodsCollector<Solution_, A, B, ResultContainer_, Intermediate_> delegate,
            Function<Intermediate_, Result_> mappingFunction) {
        this.delegate = Objects.requireNonNull(delegate);
        this.mappingFunction = Objects.requireNonNull(mappingFunction);
    }

    @Override
    public Supplier<ResultContainer_> supplier() {
        return delegate.supplier();
    }

    @Override
    public BiNeighborhoodsCollectorAccumulator<Solution_, A, B, ResultContainer_> accumulator() {
        return delegate.accumulator();
    }

    @Override
    public Function<ResultContainer_, @Nullable Result_> finisher() {
        var finisher = delegate.finisher();
        return container -> mappingFunction.apply(finisher.apply(container));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        return o instanceof AndThenBiNeighborhoodsCollector<?, ?, ?, ?, ?, ?> other
                && Objects.equals(delegate, other.delegate)
                && Objects.equals(mappingFunction, other.mappingFunction);
    }

    @Override
    public int hashCode() {
        return Objects.hash(delegate, mappingFunction);
    }

}
