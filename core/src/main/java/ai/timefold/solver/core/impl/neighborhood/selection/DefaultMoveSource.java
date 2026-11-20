package ai.timefold.solver.core.impl.neighborhood.selection;

import ai.timefold.solver.core.preview.api.neighborhood.MoveProvider;
import ai.timefold.solver.core.preview.api.neighborhood.selection.MoveSource;

import org.jspecify.annotations.NullMarked;

/**
 * @param index the position in the phase's list of move sources
 * @param id the unique ID of the move provider in the neighborhood
 * @param provider the move provider that gives the moves
 */
@NullMarked
public record DefaultMoveSource<Solution_>(int index, String id, MoveProvider<Solution_> provider)
        implements
            MoveSource<Solution_> {

    @Override
    public String toString() {
        return id;
    }

}
