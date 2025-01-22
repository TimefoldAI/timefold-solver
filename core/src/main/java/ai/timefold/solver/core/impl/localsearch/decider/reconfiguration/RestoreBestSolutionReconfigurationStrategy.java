package ai.timefold.solver.core.impl.localsearch.decider.reconfiguration;

import java.util.Objects;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.impl.localsearch.decider.LocalSearchDecider;
import ai.timefold.solver.core.impl.localsearch.scope.LocalSearchPhaseScope;
import ai.timefold.solver.core.impl.phase.scope.AbstractPhaseScope;
import ai.timefold.solver.core.impl.phase.scope.AbstractStepScope;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class RestoreBestSolutionReconfigurationStrategy<Solution_> implements ReconfigurationStrategy<Solution_> {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private LocalSearchDecider<Solution_> decider;

    public void setDecider(LocalSearchDecider<Solution_> decider) {
        this.decider = decider;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <Score_ extends Score<Score_>> Score_ apply(AbstractStepScope<Solution_> stepScope) {
        var solverScope = stepScope.getPhaseScope().getSolverScope();
        logger.debug("Resetting working solution, score ({})", solverScope.getBestScore());
        solverScope.setWorkingSolutionFromBestSolution();
        // Changing the working solution requires reinitializing the move selector and acceptor
        // 1 - The move selector will reset all cached lists using old solution entity references
        // 2 - The acceptor will restart its search from the updated working solution (last best solution)
        decider.phaseStarted((LocalSearchPhaseScope<Solution_>) stepScope.getPhaseScope());
        return (Score_) solverScope.getBestScore();
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
        Objects.requireNonNull(decider);
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
