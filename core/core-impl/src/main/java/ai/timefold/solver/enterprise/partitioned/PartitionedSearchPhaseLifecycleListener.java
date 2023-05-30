package ai.timefold.solver.enterprise.partitioned;

import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.impl.solver.event.SolverLifecycleListener;

/**
 * @param <Solution_> the solution type, the class with the {@link PlanningSolution} annotation
 */
interface PartitionedSearchPhaseLifecycleListener<Solution_> extends SolverLifecycleListener<Solution_> {

    void phaseStarted(PartitionedSearchPhaseScope<Solution_> phaseScope);

    void stepStarted(PartitionedSearchStepScope<Solution_> stepScope);

    void stepEnded(PartitionedSearchStepScope<Solution_> stepScope);

    void phaseEnded(PartitionedSearchPhaseScope<Solution_> phaseScope);

}
