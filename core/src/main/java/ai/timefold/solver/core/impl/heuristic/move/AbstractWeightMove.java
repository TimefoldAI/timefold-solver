package ai.timefold.solver.core.impl.heuristic.move;

import ai.timefold.solver.core.api.domain.solution.PlanningSolution;

/**
 * Abstract superclass for {@link Move} which includes the ability to add weight count that is used by the move count
 * speed metric.
 *
 * @param <Solution_> the solution type, the class with the {@link PlanningSolution} annotation
 * @see Move
 * @see ai.timefold.solver.core.config.solver.monitoring.SolverMetric
 */
public abstract class AbstractWeightMove<Solution_> implements Move<Solution_> {

    private int weightCount = 1;

    @Override
    public int getMoveWeightCount() {
        return weightCount;
    }

    public void setWeightCount(int weightCount) {
        this.weightCount = weightCount;
    }
}
