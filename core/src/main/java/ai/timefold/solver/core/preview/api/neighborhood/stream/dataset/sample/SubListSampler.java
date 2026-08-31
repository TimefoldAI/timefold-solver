package ai.timefold.solver.core.preview.api.neighborhood.stream.dataset.sample;

import ai.timefold.solver.core.preview.api.domain.metamodel.PlanningListVariableMetaModel;
import ai.timefold.solver.core.preview.api.move.SolutionView;
import ai.timefold.solver.core.preview.api.neighborhood.stream.MoveStreamFactory;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Draws a contiguous span ("sub-list") of a list variable, seeded by an assigned value.
 * Built only via {@link Samplers#subList(PlanningListVariableMetaModel, int, int, java.util.random.RandomGenerator)
 * Samplers.subList}.
 * The seed value only picks the entity,
 * and a fresh {@code (fromIndex, length)} is then drawn uniformly over every admissible sub-list of the entity's unpinned
 * window.
 * <p>
 * <strong>Caveat:</strong> a fully {@code @PlanningPin}-immovable entity reports {@link SolutionView#getFirstUnpinnedIndex} as
 * {@code 0}
 * even though its whole list is pinned.
 * This drawer is only safe
 * when every seed value comes from an enumeration
 * that already excludes immovable entities,
 * such as {@link MoveStreamFactory#forEachAssignedValue}.
 *
 * @param <Solution_> the solution type
 * @param <Entity_> the entity type
 * @param <Value_> the variable type
 */
@NullMarked
public interface SubListSampler<Solution_, Entity_, Value_> {

    /**
     * The default {@code maximumSubListSize} of the built-in {@code SubList*MoveProvider} no-arg constructors:
     * a sub-list relocation is blind to the score impact of the span it moves,
     * so an unbounded span makes move cost linear in list size for no corresponding benefit.
     */
    int DEFAULT_MAXIMUM_SUB_LIST_SIZE = 10;

    /**
     * @param solutionView the view of the solution the span is drawn from
     * @param seedValue an assigned value; only used to pick the entity, never anchored to its own position
     * @return a freshly drawn span over the entity's whole unpinned window, or {@code null}
     *         if that window is smaller than the minimum sub-list size
     */
    @Nullable
    Range<Entity_> byValue(SolutionView<Solution_> solutionView, Value_ seedValue);

    /**
     * @param solutionView the view of the solution the span is drawn from
     * @param entity the entity whose unpinned window the span is drawn over
     * @return a freshly drawn span over the entity's whole unpinned window,
     *         or {@code null} if that window is smaller than the minimum sub-list size
     */
    @Nullable
    Range<Entity_> byEntity(SolutionView<Solution_> solutionView, Entity_ entity);

}
