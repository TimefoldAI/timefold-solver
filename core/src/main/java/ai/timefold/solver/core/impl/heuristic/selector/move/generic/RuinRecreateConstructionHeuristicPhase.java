package ai.timefold.solver.core.impl.heuristic.selector.move.generic;

import ai.timefold.solver.core.impl.constructionheuristic.ConstructionHeuristicPhase;
import ai.timefold.solver.core.impl.constructionheuristic.DefaultConstructionHeuristicPhase;
import ai.timefold.solver.core.impl.constructionheuristic.scope.ConstructionHeuristicPhaseScope;
import ai.timefold.solver.core.impl.constructionheuristic.scope.ConstructionHeuristicStepScope;
import ai.timefold.solver.core.impl.phase.scope.AbstractStepScope;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;

final class RuinRecreateConstructionHeuristicPhase<Solution_>
        extends DefaultConstructionHeuristicPhase<Solution_>
        implements ConstructionHeuristicPhase<Solution_> {

    RuinRecreateConstructionHeuristicPhase(RuinRecreateConstructionHeuristicPhaseBuilder<Solution_> builder) {
        super(builder);
    }

    @Override
    protected void collectMetrics(AbstractStepScope<Solution_> stepScope) {
        // Nested phase doesn't collect metrics.
    }

    @Override
    protected ConstructionHeuristicPhaseScope<Solution_> buildPhaseScope(SolverScope<Solution_> solverScope, int phaseIndex) {
        return new RuinRecreateConstructionHeuristicPhaseScope<>(solverScope, phaseIndex);
    }

    @Override
    protected void processWorkingSolutionDuringStep(ConstructionHeuristicStepScope<Solution_> stepScope) {
        // Ruin and Recreate CH doesn't process the working solution, it is a nested phase.
    }

    @Override
    protected void updateBestSolutionAndFire(ConstructionHeuristicPhaseScope<Solution_> phaseScope) {
        // Ruin and Recreate CH doesn't update the best solution, it is a nested phase.
    }

    @Override
    public String getPhaseTypeString() {
        return "Ruin & Recreate Construction Heuristics";
    }

}
