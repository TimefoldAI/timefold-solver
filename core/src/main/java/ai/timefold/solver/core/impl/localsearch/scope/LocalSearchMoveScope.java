package ai.timefold.solver.core.impl.localsearch.scope;

import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.impl.heuristic.move.Move;
import ai.timefold.solver.core.impl.phase.scope.AbstractMoveScope;

/**
 * @param <Solution_> the solution type, the class with the {@link PlanningSolution} annotation
 */
public final class LocalSearchMoveScope<Solution_> extends AbstractMoveScope<Solution_> {

    private Boolean accepted = null;

    public LocalSearchMoveScope(LocalSearchStepScope<Solution_> stepScope, int moveIndex, Move<Solution_> move) {
        super(stepScope, moveIndex, move);
    }

    @Override
    public LocalSearchStepScope<Solution_> getStepScope() {
        return (LocalSearchStepScope<Solution_>) super.getStepScope();
    }

    public Boolean getAccepted() {
        return accepted;
    }

    public void setAccepted(Boolean accepted) {
        this.accepted = accepted;
    }

}
