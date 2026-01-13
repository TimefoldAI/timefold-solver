package ai.timefold.solver.core.impl.score.director;

import java.util.function.Consumer;

import ai.timefold.solver.core.impl.domain.variable.supply.SupplyManager;
import ai.timefold.solver.core.impl.neighborhood.NeighborhoodsBasedMoveRepository;

/**
 * A {@link Consumer} that notifies a {@link NeighborhoodsBasedMoveRepository} about changes to entities and variables.
 *
 * @param <Solution_> the solution type
 * @see SupplyManager#getStateChangeNotifier() Description of the uses of this class.
 */
public final class NeighborhoodNotifier<Solution_> implements Consumer<Object> {

    private boolean isTracking;
    private NeighborhoodsBasedMoveRepository<Solution_> moveRepository;

    public NeighborhoodNotifier() {
        isTracking = false;
    }

    public void setTracking(boolean isTracking) {
        this.isTracking = isTracking;
    }

    public NeighborhoodsBasedMoveRepository<Solution_> getMoveRepository() {
        return moveRepository;
    }

    public void setMoveRepository(NeighborhoodsBasedMoveRepository<Solution_> moveRepository) {
        this.moveRepository = moveRepository;
    }

    @Override
    public void accept(Object entity) {
        if (moveRepository == null || !isTracking) {
            return;
        }
        moveRepository.update(entity);
    }

}
