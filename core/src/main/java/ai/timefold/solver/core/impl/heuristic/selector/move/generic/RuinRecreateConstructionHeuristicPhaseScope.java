package ai.timefold.solver.core.impl.heuristic.selector.move.generic;

import ai.timefold.solver.core.impl.constructionheuristic.scope.ConstructionHeuristicPhaseScope;
import ai.timefold.solver.core.impl.heuristic.move.Move;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;

final class RuinRecreateConstructionHeuristicPhaseScope<Solution_> extends ConstructionHeuristicPhaseScope<Solution_> {

    public RuinRecreateConstructionHeuristicPhaseScope(SolverScope<Solution_> solverScope, int phaseIndex) {
        super(solverScope, phaseIndex);
    }

    @Override
    public void addChildThreadsMoveEvaluationCount(long addition) {
        // Nested phase does not count moves.
    }

    @Override
    public void addMoveEvaluationCount(Move<?> move, long count) {
        // Nested phase does not count moves.
    }

}
