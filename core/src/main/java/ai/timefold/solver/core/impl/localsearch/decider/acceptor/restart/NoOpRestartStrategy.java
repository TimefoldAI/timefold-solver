package ai.timefold.solver.core.impl.localsearch.decider.acceptor.restart;

import ai.timefold.solver.core.impl.localsearch.scope.LocalSearchMoveScope;
import ai.timefold.solver.core.impl.localsearch.scope.LocalSearchPhaseScope;
import ai.timefold.solver.core.impl.localsearch.scope.LocalSearchStepScope;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;

public class NoOpRestartStrategy<Solution_> implements RestartStrategy<Solution_> {

    @Override
    public boolean isTriggered(LocalSearchMoveScope<Solution_> moveScope) {
        return false;
    }

    @Override
    public void reset(LocalSearchMoveScope<Solution_> moveScope) {
        // Do nothing
    }

    @Override
    public void phaseStarted(LocalSearchPhaseScope<Solution_> phaseScope) {
        // Do nothing
    }

    @Override
    public void stepStarted(LocalSearchStepScope<Solution_> stepScope) {
        // Do nothing
    }

    @Override
    public void stepEnded(LocalSearchStepScope<Solution_> stepScope) {
        // Do nothing
    }

    @Override
    public void phaseEnded(LocalSearchPhaseScope<Solution_> phaseScope) {
        // Do nothing
    }

    @Override
    public void solvingStarted(SolverScope<Solution_> solverScope) {
        // Do nothing
    }

    @Override
    public void solvingEnded(SolverScope<Solution_> solverScope) {
        // Do nothing
    }
}
