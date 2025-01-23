package ai.timefold.solver.core.impl.localsearch.decider.acceptor;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.impl.localsearch.decider.acceptor.restart.RestartStrategy;
import ai.timefold.solver.core.impl.localsearch.scope.LocalSearchMoveScope;
import ai.timefold.solver.core.impl.localsearch.scope.LocalSearchPhaseScope;
import ai.timefold.solver.core.impl.localsearch.scope.LocalSearchStepScope;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;

/**
 * Base class designed to analyze whether the solving process needs to be restarted.
 * Additionally, it also calls a reconfiguration logic as a result of restarting the solving process.
 */
public abstract class ReconfigurableAcceptor<Solution_> extends AbstractAcceptor<Solution_> {

    private final RestartStrategy<Solution_> restartStrategy;
    private final boolean enabled;

    protected ReconfigurableAcceptor(boolean enabled, RestartStrategy<Solution_> restartStrategy) {
        this.enabled = enabled;
        this.restartStrategy = restartStrategy;
    }

    protected boolean isEnabled() {
        return enabled;
    }

    @Override
    public void solvingStarted(SolverScope<Solution_> solverScope) {
        super.solvingStarted(solverScope);
        restartStrategy.solvingStarted(solverScope);
    }

    @Override
    public void phaseStarted(LocalSearchPhaseScope<Solution_> phaseScope) {
        super.phaseStarted(phaseScope);
        restartStrategy.phaseStarted(phaseScope);
    }

    @Override
    public void phaseEnded(LocalSearchPhaseScope<Solution_> phaseScope) {
        super.phaseEnded(phaseScope);
        restartStrategy.phaseEnded(phaseScope);
    }

    @Override
    public void stepStarted(LocalSearchStepScope<Solution_> stepScope) {
        super.stepStarted(stepScope);
        restartStrategy.stepStarted(stepScope);
    }

    @Override
    public void stepEnded(LocalSearchStepScope<Solution_> stepScope) {
        super.stepEnded(stepScope);
        restartStrategy.stepEnded(stepScope);
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean isAccepted(LocalSearchMoveScope<Solution_> moveScope) {
        if (enabled && restartStrategy.isTriggered(moveScope)) {
            moveScope.getStepScope().getPhaseScope().triggerReconfiguration();
            return true;
        }
        var accepted = evaluate(moveScope);
        var improved = enabled && moveScope.getScore().compareTo(moveScope.getStepScope().getPhaseScope().getBestScore()) > 0;
        if (improved) {
            restartStrategy.reset(moveScope);
        }
        return accepted;
    }

    protected abstract boolean evaluate(LocalSearchMoveScope<Solution_> moveScope);

    protected abstract <Score_ extends Score<Score_>> void reset(Score_ score);
}
