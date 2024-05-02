package ai.timefold.solver.core.impl.constructionheuristic.scope;

import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.impl.heuristic.move.Move;
import ai.timefold.solver.core.impl.phase.scope.AbstractMoveScope;

/**
 * @param <Solution_> the solution type, the class with the {@link PlanningSolution} annotation
 */
public final class ConstructionHeuristicMoveScope<Solution_> extends AbstractMoveScope<Solution_> {

    public ConstructionHeuristicMoveScope(ConstructionHeuristicStepScope<Solution_> stepScope,
            int moveIndex, Move<Solution_> move) {
        super(stepScope, moveIndex, move);
    }

    @Override
    public ConstructionHeuristicStepScope<Solution_> getStepScope() {
        return (ConstructionHeuristicStepScope<Solution_>) super.getStepScope();
    }

}
