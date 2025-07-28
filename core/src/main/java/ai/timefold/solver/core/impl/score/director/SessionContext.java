package ai.timefold.solver.core.impl.score.director;

import ai.timefold.solver.core.impl.domain.variable.supply.SupplyManager;
import ai.timefold.solver.core.preview.api.move.SolutionView;

import org.jspecify.annotations.NullMarked;

/**
 * Used throughout the solver to provide access to some internals of the score director
 * required for initialization of Bavet nodes and other components.
 * It is imperative that all of these values come from the same score director instance,
 * because they are all tied to the same working solution.
 * These values are only valid during the runtime of the session;
 * new session requires a new {@link SessionContext} instance.
 */
@NullMarked
public record SessionContext<Solution_>(Solution_ workingSolution, SolutionView<Solution_> solutionView,
        ValueRangeManager<Solution_> valueRangeManager, SupplyManager supplyManager) {

    public SessionContext(InnerScoreDirector<Solution_, ?> scoreDirector) {
        this(scoreDirector.getWorkingSolution(), scoreDirector.getMoveDirector(),
                scoreDirector.getValueRangeManager(), scoreDirector.getSupplyManager());
    }

}
