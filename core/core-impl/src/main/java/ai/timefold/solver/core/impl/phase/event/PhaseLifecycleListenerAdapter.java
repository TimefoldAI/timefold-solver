package ai.timefold.solver.core.impl.phase.event;

import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.impl.phase.scope.AbstractPhaseScope;
import ai.timefold.solver.core.impl.phase.scope.AbstractStepScope;
import ai.timefold.solver.core.impl.solver.event.SolverLifecycleListenerAdapter;

/**
 * An adapter for {@link PhaseLifecycleListener}.
 *
 * @param <Solution_> the solution type, the class with the {@link PlanningSolution} annotation
 */
public abstract class PhaseLifecycleListenerAdapter<Solution_> extends SolverLifecycleListenerAdapter<Solution_>
        implements PhaseLifecycleListener<Solution_> {

    @Override
    public void phaseStarted(AbstractPhaseScope<Solution_> phaseScope) {
        // Hook method
    }

    @Override
    public void stepStarted(AbstractStepScope<Solution_> stepScope) {
        // Hook method
    }

    @Override
    public void stepEnded(AbstractStepScope<Solution_> stepScope) {
        // Hook method
    }

    @Override
    public void phaseEnded(AbstractPhaseScope<Solution_> phaseScope) {
        // Hook method
    }

}
