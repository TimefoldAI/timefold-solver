package ai.timefold.solver.core.impl.phase.custom.scope;

import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.impl.phase.scope.AbstractPhaseScope;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;

/**
 * @param <Solution_> the solution type, the class with the {@link PlanningSolution} annotation
 */
public final class CustomPhaseScope<Solution_> extends AbstractPhaseScope<Solution_> {

    private CustomStepScope<Solution_> lastCompletedStepScope;

    public CustomPhaseScope(SolverScope<Solution_> solverScope, int phaseIndex) {
        this(solverScope, phaseIndex, false);
    }

    public CustomPhaseScope(SolverScope<Solution_> solverScope, int phaseIndex, boolean phaseSendsBestSolutionEvents) {
        super(solverScope, phaseIndex, phaseSendsBestSolutionEvents);
        lastCompletedStepScope = new CustomStepScope<>(this, -1);
    }

    @Override
    public CustomStepScope<Solution_> getLastCompletedStepScope() {
        return lastCompletedStepScope;
    }

    public void setLastCompletedStepScope(CustomStepScope<Solution_> lastCompletedStepScope) {
        this.lastCompletedStepScope = lastCompletedStepScope;
    }

    // ************************************************************************
    // Calculated methods
    // ************************************************************************

}
