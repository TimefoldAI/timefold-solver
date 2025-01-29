package ai.timefold.solver.core.impl.localsearch.decider.reconfiguration;

import java.util.Objects;

import ai.timefold.solver.core.impl.localsearch.decider.LocalSearchDecider;
import ai.timefold.solver.core.impl.localsearch.scope.LocalSearchPhaseScope;
import ai.timefold.solver.core.impl.localsearch.scope.LocalSearchStepScope;
import ai.timefold.solver.core.impl.phase.scope.AbstractPhaseScope;
import ai.timefold.solver.core.impl.phase.scope.AbstractStepScope;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class RestoreBestSolutionRestartStrategy<Solution_> implements RestartStrategy<Solution_> {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private LocalSearchDecider<Solution_> decider;

    @Override
    public void applyRestart(AbstractStepScope<Solution_> stepScope) {
        var solverScope = stepScope.getPhaseScope().getSolverScope();
        logger.trace("Resetting working solution, score ({})", solverScope.getBestScore());
        decider.setWorkingSolutionFromBestSolution((LocalSearchStepScope<Solution_>) stepScope);
        // Mark the solver as unstuck as the best solution is already restored
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
        this.decider = Objects.requireNonNull(((LocalSearchPhaseScope<Solution_>) phaseScope).getDecider());
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
