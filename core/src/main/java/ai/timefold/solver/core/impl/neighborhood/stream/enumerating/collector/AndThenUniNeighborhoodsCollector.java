package ai.timefold.solver.core.impl.neighborhood.stream.enumerating.collector;

import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;

import ai.timefold.solver.core.preview.api.neighborhood.stream.enumerating.collector.UniNeighborhoodsCollector;
import ai.timefold.solver.core.preview.api.neighborhood.stream.enumerating.collector.UniNeighborhoodsCollectorAccumulator;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public final class AndThenUniNeighborhoodsCollector<Solution_, A, ResultContainer_, Intermediate_, Result_>
        implements UniNeighborhoodsCollector<Solution_, A, ResultContainer_, Result_> {

    private final UniNeighborhoodsCollector<Solution_, A, ResultContainer_, Intermediate_> delegate;
    private final Function<@Nullable Intermediate_, @Nullable Result_> mappingFunction;

    public AndThenUniNeighborhoodsCollector(
            UniNeighborhoodsCollector<Solution_, A, ResultContainer_, Intermediate_> delegate,
            Function<Intermediate_, Result_> mappingFunction) {
        this.delegate = Objects.requireNonNull(delegate);
        this.mappingFunction = Objects.requireNonNull(mappingFunction);
    }

    @Override
    public Supplier<ResultContainer_> supplier() {
        return delegate.supplier();
    }

    @Override
    public UniNeighborhoodsCollectorAccumulator<Solution_, A, ResultContainer_> accumulator() {
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
        return o instanceof AndThenUniNeighborhoodsCollector<?, ?, ?, ?, ?> other
                && Objects.equals(delegate, other.delegate)
                && Objects.equals(mappingFunction, other.mappingFunction);
    }

    @Override
    public int hashCode() {
        return Objects.hash(delegate, mappingFunction);
    }

}
