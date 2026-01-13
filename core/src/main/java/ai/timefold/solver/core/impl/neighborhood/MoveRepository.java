package ai.timefold.solver.core.impl.neighborhood;

import ai.timefold.solver.core.impl.phase.event.PhaseLifecycleListener;
import ai.timefold.solver.core.impl.score.director.SessionContext;
import ai.timefold.solver.core.preview.api.move.Move;

import org.jspecify.annotations.NullMarked;

/**
 * This is a shared abstraction for all three types of move iterators currently used in the solver:
 * 
 * <ul>
 * <li>{@link MoveSelectorBasedMoveRepository} for local search and exhaustive search.</li>
 * <li>{@link PlacerBasedMoveRepository} for construction heuristics.</li>
 * <li>{@link NeighborhoodsBasedMoveRepository} for the Neighborhoods API.</li>
 * </ul>
 * 
 * As the Neighborhoods API becomes gradually more capable,
 * these extra implementations will be removed
 * until only {@link NeighborhoodsBasedMoveRepository} remains in use.
 * At this point, this entire abstraction will be removed,
 * and all code will work with Neighborhoods directly.
 * 
 * @param <Solution_>
 */
@NullMarked
public sealed interface MoveRepository<Solution_>
        extends Iterable<Move<Solution_>>, PhaseLifecycleListener<Solution_>
        permits NeighborhoodsBasedMoveRepository, MoveSelectorBasedMoveRepository, PlacerBasedMoveRepository {

    boolean isNeverEnding();

    void initialize(SessionContext<Solution_> context);

}
