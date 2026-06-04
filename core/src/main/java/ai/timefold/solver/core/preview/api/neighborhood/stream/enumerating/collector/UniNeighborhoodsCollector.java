package ai.timefold.solver.core.preview.api.neighborhood.stream.enumerating.collector;

import java.util.function.Function;
import java.util.function.Supplier;

import ai.timefold.solver.core.preview.api.neighborhood.stream.enumerating.UniEnumeratingStream;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Collects the facts from a {@link UniEnumeratingStream} group into a result.
 * Used with {@link UniEnumeratingStream#groupBy}.
 * <p>
 * Custom implementations should implement {@link Object#equals(Object)} and {@link Object#hashCode()}
 * based on their fields to allow node sharing.
 *
 * @param <Solution_> the type of the solution
 * @param <A> the type of the only fact in the source stream's tuple
 * @param <ResultContainer_> the mutable accumulation type (often hidden as an implementation detail)
 * @param <Result_> the type of the result
 */
@NullMarked
public interface UniNeighborhoodsCollector<Solution_, A, ResultContainer_, Result_> {

    Supplier<ResultContainer_> supplier();

    UniNeighborhoodsCollectorAccumulator<Solution_, A, ResultContainer_> accumulator();

    Function<ResultContainer_, @Nullable Result_> finisher();

}
