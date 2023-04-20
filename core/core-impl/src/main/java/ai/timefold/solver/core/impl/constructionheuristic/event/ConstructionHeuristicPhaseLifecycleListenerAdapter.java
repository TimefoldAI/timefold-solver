package ai.timefold.solver.core.impl.constructionheuristic.event;

import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.impl.constructionheuristic.scope.ConstructionHeuristicPhaseScope;
import ai.timefold.solver.core.impl.constructionheuristic.scope.ConstructionHeuristicStepScope;
import ai.timefold.solver.core.impl.solver.event.SolverLifecycleListenerAdapter;

/**
 * An adapter for {@link ConstructionHeuristicPhaseLifecycleListener}.
 *
 * @param <Solution_> the solution type, the class with the {@link PlanningSolution} annotation
 */
public class ConstructionHeuristicPhaseLifecycleListenerAdapter<Solution_> extends SolverLifecycleListenerAdapter<Solution_>
        implements ConstructionHeuristicPhaseLifecycleListener<Solution_> {

    @Override
    public void phaseStarted(ConstructionHeuristicPhaseScope<Solution_> phaseScope) {
        // Hook method
    }

    @Override
    public void stepStarted(ConstructionHeuristicStepScope<Solution_> stepScope) {
        // Hook method
    }

    @Override
    public void stepEnded(ConstructionHeuristicStepScope<Solution_> stepScope) {
        // Hook method
    }

    @Override
    public void phaseEnded(ConstructionHeuristicPhaseScope<Solution_> phaseScope) {
        // Hook method
    }

}
