package ai.timefold.solver.core.impl.localsearch.scope;

import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.impl.phase.scope.AbstractPhaseScope;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;

/**
 * @param <Solution_> the solution type, the class with the {@link PlanningSolution} annotation
 */
public final class LocalSearchPhaseScope<Solution_> extends AbstractPhaseScope<Solution_> {

    private LocalSearchStepScope<Solution_> lastCompletedStepScope;

    public LocalSearchPhaseScope(SolverScope<Solution_> solverScope, int phaseIndex) {
        super(solverScope, phaseIndex);
        lastCompletedStepScope = new LocalSearchStepScope<>(this, -1);
        lastCompletedStepScope.setTimeGradient(0.0);
    }

    @Override
    public LocalSearchStepScope<Solution_> getLastCompletedStepScope() {
        return lastCompletedStepScope;
    }

    public void setLastCompletedStepScope(LocalSearchStepScope<Solution_> lastCompletedStepScope) {
        this.lastCompletedStepScope = lastCompletedStepScope;
    }

    // ************************************************************************
    // Calculated methods
    // ************************************************************************

}
