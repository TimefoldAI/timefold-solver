package ai.timefold.solver.core.impl.move;

import ai.timefold.solver.core.impl.domain.variable.supply.SupplyManager;
import ai.timefold.solver.core.impl.phase.event.PhaseLifecycleListener;
import ai.timefold.solver.core.preview.api.move.Move;

import org.jspecify.annotations.NullMarked;

/**
 * This is a shared abstraction for all three types of move iterators currently used in the solver:
 * 
 * <ul>
 * <li>{@link MoveSelectorBasedMoveRepository} for local search and exhaustive search.</li>
 * <li>{@link PlacerBasedMoveRepository} for construction heuristics.</li>
 * <li>{@link MoveStreamsBasedMoveRepository} for move streams.</li>
 * </ul>
 * 
 * As move streams become gradually more capable,
 * these extra implementations will be removed
 * until only {@link MoveStreamsBasedMoveRepository} remains in use.
 * At this point, this entire abstraction will be removed,
 * and all code will work with move streams directly.
 * 
 * @param <Solution_>
 */
@NullMarked
public sealed interface MoveRepository<Solution_>
        extends Iterable<Move<Solution_>>, PhaseLifecycleListener<Solution_>
        permits MoveSelectorBasedMoveRepository, MoveStreamsBasedMoveRepository, PlacerBasedMoveRepository {

    boolean isNeverEnding();

    void initialize(Solution_ workingSolution, SupplyManager supplyManager);

}
