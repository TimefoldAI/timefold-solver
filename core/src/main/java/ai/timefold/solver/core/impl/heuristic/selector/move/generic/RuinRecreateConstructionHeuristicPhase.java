package ai.timefold.solver.core.impl.heuristic.selector.move.generic;

import ai.timefold.solver.core.impl.constructionheuristic.ConstructionHeuristicPhase;
import ai.timefold.solver.core.impl.constructionheuristic.DefaultConstructionHeuristicPhase;
import ai.timefold.solver.core.impl.constructionheuristic.scope.ConstructionHeuristicPhaseScope;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;

final class RuinRecreateConstructionHeuristicPhase<Solution_>
        extends DefaultConstructionHeuristicPhase<Solution_>
        implements ConstructionHeuristicPhase<Solution_> {

    RuinRecreateConstructionHeuristicPhase(RuinRecreateConstructionHeuristicPhaseBuilder<Solution_> builder) {
        super(builder);
    }

    @Override
    protected ConstructionHeuristicPhaseScope<Solution_> buildPhaseScope(SolverScope<Solution_> solverScope, int phaseIndex) {
        return new RuinRecreateConstructionHeuristicPhaseScope<>(solverScope, phaseIndex);
    }

    @Override
    protected boolean isNested() {
        return true;
    }

    @Override
    public String getPhaseTypeString() {
        return "Ruin & Recreate Construction Heuristics";
    }

}
