package ai.timefold.solver.core.impl.localsearch.decider.acceptor;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.impl.localsearch.decider.acceptor.reconfiguration.ReconfigurationStrategy;
import ai.timefold.solver.core.impl.localsearch.scope.LocalSearchMoveScope;
import ai.timefold.solver.core.impl.localsearch.scope.LocalSearchPhaseScope;
import ai.timefold.solver.core.impl.localsearch.scope.LocalSearchStepScope;

/**
 * Base class designed to provide a reconfiguration strategy for an acceptor implementation.
 */
public abstract class ReconfigurableAbstractAcceptor<Solution_> extends AbstractAcceptor<Solution_> {

    private final ReconfigurationStrategy<Solution_> reconfigurationStrategy;
    private final boolean enabled;

    protected ReconfigurableAbstractAcceptor(ReconfigurationStrategy<Solution_> reconfigurationStrategy, boolean enabled) {
        this.reconfigurationStrategy = reconfigurationStrategy;
        this.enabled = enabled;
    }

    @Override
    public void phaseStarted(LocalSearchPhaseScope<Solution_> phaseScope) {
        super.phaseStarted(phaseScope);
        reconfigurationStrategy.phaseStarted(phaseScope);
    }

    @Override
    public void phaseEnded(LocalSearchPhaseScope<Solution_> phaseScope) {
        super.phaseEnded(phaseScope);
        reconfigurationStrategy.phaseEnded(phaseScope);
    }

    @Override
    public void stepStarted(LocalSearchStepScope<Solution_> stepScope) {
        super.stepStarted(stepScope);
        reconfigurationStrategy.stepStarted(stepScope);
    }

    @Override
    public void stepEnded(LocalSearchStepScope<Solution_> stepScope) {
        super.stepEnded(stepScope);
        reconfigurationStrategy.stepEnded(stepScope);
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean isAccepted(LocalSearchMoveScope<Solution_> moveScope) {
        if (enabled && reconfigurationStrategy.needReconfiguration(moveScope)) {
            moveScope.getStepScope().getPhaseScope().triggerReconfiguration();
            return true;
        }
        var accepted = evaluate(moveScope);
        if (enabled && moveScope.getScore().compareTo(moveScope.getStepScope().getPhaseScope().getBestScore()) > 0) {
            reconfigurationStrategy.reset();
        }
        return accepted;
    }

    protected abstract boolean evaluate(LocalSearchMoveScope<Solution_> moveScope);

    protected abstract <Score_ extends Score<Score_>> void reset(Score_ score);
}
