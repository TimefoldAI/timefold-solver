package ai.timefold.solver.core.impl.constructionheuristic.scope;

import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.impl.phase.scope.AbstractPhaseScope;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;

/**
 * @param <Solution_> the solution type, the class with the {@link PlanningSolution} annotation
 */
public final class ConstructionHeuristicPhaseScope<Solution_> extends AbstractPhaseScope<Solution_> {

    private ConstructionHeuristicStepScope<Solution_> lastCompletedStepScope;

    public ConstructionHeuristicPhaseScope(SolverScope<Solution_> solverScope, int phaseIndex) {
        super(solverScope, phaseIndex, false);
        lastCompletedStepScope = new ConstructionHeuristicStepScope<>(this, -1);
    }

    @Override
    public ConstructionHeuristicStepScope<Solution_> getLastCompletedStepScope() {
        return lastCompletedStepScope;
    }

    public void setLastCompletedStepScope(ConstructionHeuristicStepScope<Solution_> lastCompletedStepScope) {
        this.lastCompletedStepScope = lastCompletedStepScope;
    }

    // ************************************************************************
    // Calculated methods
    // ************************************************************************

}
