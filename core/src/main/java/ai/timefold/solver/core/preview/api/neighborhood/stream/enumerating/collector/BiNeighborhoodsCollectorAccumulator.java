package ai.timefold.solver.core.preview.api.neighborhood.stream.enumerating.collector;

import ai.timefold.solver.core.preview.api.move.SolutionView;

import org.jspecify.annotations.NullMarked;

/**
 * As defined by {@link UniNeighborhoodsCollectorAccumulator}, only for {@link BiNeighborhoodsCollector}.
 *
 * @param <Solution_> the type of the solution
 * @param <A> the type of the first fact in the source stream's tuple
 * @param <B> the type of the second fact in the source stream's tuple
 * @param <ResultContainer_> the mutable accumulation type
 */
@NullMarked
public interface BiNeighborhoodsCollectorAccumulator<Solution_, A, B, ResultContainer_> {

    BiNeighborhoodsCollectorValueHandle<A, B> intoGroup(SolutionView<Solution_> view, ResultContainer_ container);

}
