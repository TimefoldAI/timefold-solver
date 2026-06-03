package ai.timefold.solver.core.preview.api.neighborhood.stream.collector;

import ai.timefold.solver.core.preview.api.move.SolutionView;

import org.jspecify.annotations.NullMarked;

/**
 * Accumulates values into a group container for a {@link UniNeighborhoodsCollector}.
 * Called once per group to obtain a {@link UniNeighborhoodsCollectorValueHandle}
 * for inserting, updating, and removing values.
 *
 * @param <Solution_> the type of the solution
 * @param <A> the type of the only fact in the source stream's tuple
 * @param <ResultContainer_> the mutable accumulation type
 */
@NullMarked
public interface UniNeighborhoodsCollectorAccumulator<Solution_, A, ResultContainer_> {

    /**
     * Called when a new value enters the group.
     * The returned handle is used to insert the value ({@link UniNeighborhoodsCollectorValueHandle#add}),
     * update it ({@link UniNeighborhoodsCollectorValueHandle#replaceWith}),
     * and remove it ({@link UniNeighborhoodsCollectorValueHandle#remove}).
     *
     * @param view read-only access to the current working solution
     * @param container the group's accumulation container
     * @return a handle for the value in the group
     */
    UniNeighborhoodsCollectorValueHandle<A> intoGroup(SolutionView<Solution_> view, ResultContainer_ container);

}
