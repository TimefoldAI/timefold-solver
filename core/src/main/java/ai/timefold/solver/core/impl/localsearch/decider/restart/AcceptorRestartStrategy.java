package ai.timefold.solver.core.impl.localsearch.decider.restart;

import ai.timefold.solver.core.impl.localsearch.decider.acceptor.Acceptor;
import ai.timefold.solver.core.impl.localsearch.decider.acceptor.RestartableAcceptor;
import ai.timefold.solver.core.impl.localsearch.scope.LocalSearchStepScope;
import ai.timefold.solver.core.impl.phase.scope.AbstractPhaseScope;
import ai.timefold.solver.core.impl.phase.scope.AbstractStepScope;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;

public final class AcceptorRestartStrategy<Solution_> implements RestartStrategy<Solution_> {

    private final Acceptor<Solution_> acceptor;

    public AcceptorRestartStrategy(Acceptor<Solution_> acceptor) {
        this.acceptor = acceptor;
    }

    @Override
    public void applyRestart(AbstractStepScope<Solution_> stepScope) {
        if (acceptor instanceof RestartableAcceptor<Solution_> restartableAcceptor) {
            restartableAcceptor.restart((LocalSearchStepScope<Solution_>) stepScope);
        }
        // Mark the solver as unstuck as the acceptor restart logic was triggered
        stepScope.getPhaseScope().setSolverStuck(false);
    }

    @Override
    public void stepStarted(AbstractStepScope<Solution_> stepScope) {
        // Do nothing
    }

    @Override
    public void stepEnded(AbstractStepScope<Solution_> stepScope) {
        // Do nothing
    }

    @Override
    public void phaseStarted(AbstractPhaseScope<Solution_> phaseScope) {
        // Do nothing
    }

    @Override
    public void phaseEnded(AbstractPhaseScope<Solution_> phaseScope) {
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
