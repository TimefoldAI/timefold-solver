package ai.timefold.solver.core.impl.heuristic.selector.move.generic;

import ai.timefold.solver.core.impl.constructionheuristic.scope.ConstructionHeuristicPhaseScope;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;
import ai.timefold.solver.core.preview.api.move.Move;

final class RuinRecreateConstructionHeuristicPhaseScope<Solution_> extends ConstructionHeuristicPhaseScope<Solution_> {

    public RuinRecreateConstructionHeuristicPhaseScope(SolverScope<Solution_> solverScope, int phaseIndex) {
        super(solverScope, phaseIndex);
    }

    @Override
    public void addMoveEvaluationCount(Move<Solution_> move, long count) {
        // Nested phase does not count moves.
    }

}
