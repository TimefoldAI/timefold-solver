package ai.timefold.solver.core.impl.score.director;

import ai.timefold.solver.core.impl.domain.variable.supply.SupplyManager;

/**
 * Used throughout the solver to provide access to some internals of the score director
 * required for initialization of Bavet nodes and other components.
 * It is imperative that all of these values come from the same score director instance,
 * at the same time - because they are all tied to the same working solution.
 */
public record SessionContext<Solution_>(Solution_ workingSolution, ValueRangeManager<Solution_> valueRangeManager,
        SupplyManager supplyManager) {

}
