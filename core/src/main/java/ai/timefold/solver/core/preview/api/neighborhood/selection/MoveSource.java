package ai.timefold.solver.core.preview.api.neighborhood.selection;

import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.preview.api.neighborhood.MoveProvider;
import ai.timefold.solver.core.preview.api.neighborhood.NeighborhoodBuilder;

import org.jspecify.annotations.NullMarked;

/**
 * A {@link MoveProvider} as registered in the neighborhood, with its unique ID.
 * <p>
 * Only the solver implements this interface.
 * The instance is the same for the whole phase run, so a strategy may keep it.
 *
 * @param <Solution_> the solution type, the class with the {@link PlanningSolution} annotation
 */
@NullMarked
public interface MoveSource<Solution_> {

    /**
     * @return unique in the neighborhood; see {@link NeighborhoodBuilder#add(String, MoveProvider)}
     */
    String id();

    MoveProvider<Solution_> provider();

}
