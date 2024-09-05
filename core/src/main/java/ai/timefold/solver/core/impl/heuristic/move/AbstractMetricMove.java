package ai.timefold.solver.core.impl.heuristic.move;

import ai.timefold.solver.core.api.domain.solution.PlanningSolution;

/**
 * Abstract superclass for {@link Move} which includes the ability to enable or disable the move metric collection.
 *
 * @param <Solution_> the solution type, the class with the {@link PlanningSolution} annotation
 */
public abstract class AbstractMetricMove<Solution_> implements Move<Solution_> {

    private boolean collectMetricEnabled = true;

    @Override
    public boolean isCollectMetricEnabled() {
        return collectMetricEnabled;
    }

    public void setCollectMetricEnabled(boolean collectMetricEnabled) {
        this.collectMetricEnabled = collectMetricEnabled;
    }
}
