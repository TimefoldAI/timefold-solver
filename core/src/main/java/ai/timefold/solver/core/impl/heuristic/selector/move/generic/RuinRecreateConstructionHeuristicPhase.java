package ai.timefold.solver.core.impl.heuristic.selector.move.generic;

import ai.timefold.solver.core.impl.constructionheuristic.ConstructionHeuristicPhase;
import ai.timefold.solver.core.impl.constructionheuristic.DefaultConstructionHeuristicPhase;
import ai.timefold.solver.core.impl.constructionheuristic.scope.ConstructionHeuristicPhaseScope;
import ai.timefold.solver.core.impl.constructionheuristic.scope.ConstructionHeuristicStepScope;

final class RuinRecreateConstructionHeuristicPhase<Solution_>
        extends DefaultConstructionHeuristicPhase<Solution_>
        implements ConstructionHeuristicPhase<Solution_> {

    RuinRecreateConstructionHeuristicPhase(RuinRecreateConstructionHeuristicPhaseBuilder<Solution_> builder) {
        super(builder);
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
