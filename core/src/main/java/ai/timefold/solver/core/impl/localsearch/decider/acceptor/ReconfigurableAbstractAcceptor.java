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

    private static final int MAX_PERTURBATIONS = 2;
    private final ReconfigurationStrategy<Solution_> reconfigurationStrategy;
    private int currentPerturbationCount;
    private int remainingPerturbations;
    private final boolean enabled;

    protected ReconfigurableAbstractAcceptor(ReconfigurationStrategy<Solution_> reconfigurationStrategy, boolean enabled) {
        this.reconfigurationStrategy = reconfigurationStrategy;
        this.enabled = enabled;
    }

    @Override
    public void phaseStarted(LocalSearchPhaseScope<Solution_> phaseScope) {
        super.phaseStarted(phaseScope);
        init();
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
        if (enabled && remainingPerturbations > 0) {
            remainingPerturbations--;
            if (remainingPerturbations == 0) {
                reconfigurationStrategy.reset();
                reset(moveScope.getScore());
            }
            return true;
        }
        if (enabled && reconfigurationStrategy.needReconfiguration(moveScope)) {
            applyPerturbation(moveScope);
            return true;
        }
        var accepted = evaluate(moveScope);
        if (enabled && moveScope.getScore().compareTo(moveScope.getStepScope().getPhaseScope().getBestScore()) > 0) {
            init();
            reconfigurationStrategy.reset();
        }
        return accepted;
    }

    private void applyPerturbation(LocalSearchMoveScope<Solution_> moveScope) {
        remainingPerturbations = currentPerturbationCount;
        if (currentPerturbationCount < MAX_PERTURBATIONS) {
            currentPerturbationCount++;
        }
        moveScope.getStepScope().getPhaseScope().getSolverScope().triggerResetWorkingSolution();
    }

    private void init() {
        this.currentPerturbationCount = 1;
        this.remainingPerturbations = 0;
    }

    protected abstract boolean evaluate(LocalSearchMoveScope<Solution_> moveScope);

    protected abstract <Score_ extends Score<Score_>> void reset(Score_ score);
}
