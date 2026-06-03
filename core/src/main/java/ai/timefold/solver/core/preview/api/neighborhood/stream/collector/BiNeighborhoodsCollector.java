package ai.timefold.solver.core.preview.api.neighborhood.stream.collector;

import java.util.function.Function;
import java.util.function.Supplier;

import ai.timefold.solver.core.preview.api.neighborhood.stream.enumerating.BiEnumeratingStream;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * As defined by {@link UniNeighborhoodsCollector}, only for {@link BiEnumeratingStream}.
 *
 * @param <Solution_> the type of the solution
 * @param <A> the type of the first fact in the source stream's tuple
 * @param <B> the type of the second fact in the source stream's tuple
 * @param <ResultContainer_> the mutable accumulation type (often hidden as an implementation detail)
 * @param <Result_> the type of the result
 */
@NullMarked
public interface BiNeighborhoodsCollector<Solution_, A, B, ResultContainer_, Result_> {

    Supplier<ResultContainer_> supplier();

    BiNeighborhoodsCollectorAccumulator<Solution_, A, B, ResultContainer_> accumulator();

    Function<ResultContainer_, @Nullable Result_> finisher();

}
