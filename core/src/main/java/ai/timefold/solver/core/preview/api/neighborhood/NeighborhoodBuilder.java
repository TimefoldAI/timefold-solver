package ai.timefold.solver.core.preview.api.neighborhood;

import ai.timefold.solver.core.preview.api.domain.metamodel.PlanningSolutionMetaModel;
import ai.timefold.solver.core.preview.api.neighborhood.selection.MoveSelectionStrategy;

import org.jspecify.annotations.NullMarked;

@NullMarked
public interface NeighborhoodBuilder<Solution_> {

    PlanningSolutionMetaModel<Solution_> getSolutionMetaModel();

    /**
     * As {@link #add(String, MoveProvider)}, with {@link MoveProvider#getId()} as the ID.
     */
    default NeighborhoodBuilder<Solution_> add(MoveProvider<Solution_> moveProvider) {
        return add(moveProvider.getId(), moveProvider);
    }

    /**
     * Registers the move provider under the given ID.
     * The IDs of a neighborhood must be unique;
     * adding the same provider again with the same ID has no effect.
     * {@link MoveSelectionStrategy Move selection strategies} tell the providers apart by their ID.
     *
     * @param id not blank
     */
    NeighborhoodBuilder<Solution_> add(String id, MoveProvider<Solution_> moveProvider);

    Neighborhood build();

}
