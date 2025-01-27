package ai.timefold.solver.core.impl.localsearch.decider.reconfiguration;

import ai.timefold.solver.core.impl.phase.event.PhaseLifecycleListener;
import ai.timefold.solver.core.impl.phase.scope.AbstractStepScope;

public sealed interface ReconfigurationStrategy<Solution_> extends PhaseLifecycleListener<Solution_>
        permits RestoreBestSolutionReconfigurationStrategy {

    void apply(AbstractStepScope<Solution_> stepScope);

    default boolean isTriggered(AbstractStepScope<Solution_> stepScope) {
        return stepScope.getPhaseScope().isReconfigurationTriggered();
    }

}
