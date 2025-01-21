package ai.timefold.solver.core.impl.localsearch.decider.perturbation;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.impl.phase.event.PhaseLifecycleListener;
import ai.timefold.solver.core.impl.phase.scope.AbstractStepScope;

public interface PerturbationStrategy<Solution_> extends PhaseLifecycleListener<Solution_> {

    <Score_ extends Score<Score_>> Score_ apply(AbstractStepScope<Solution_> stepScope);

    default boolean isTriggered(AbstractStepScope<Solution_> stepScope) {
        return stepScope.getPhaseScope().isReconfigurationTriggered();
    }

}
