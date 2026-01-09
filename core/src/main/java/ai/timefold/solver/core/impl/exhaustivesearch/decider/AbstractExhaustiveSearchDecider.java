package ai.timefold.solver.core.impl.exhaustivesearch.decider;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.impl.exhaustivesearch.event.ExhaustiveSearchPhaseLifecycleListener;
import ai.timefold.solver.core.impl.exhaustivesearch.node.bounder.ScoreBounder;
import ai.timefold.solver.core.impl.exhaustivesearch.scope.ExhaustiveSearchPhaseScope;
import ai.timefold.solver.core.impl.exhaustivesearch.scope.ExhaustiveSearchStepScope;
import ai.timefold.solver.core.impl.neighborhood.MoveRepository;
import ai.timefold.solver.core.impl.solver.recaller.BestSolutionRecaller;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;
import ai.timefold.solver.core.impl.solver.termination.PhaseTermination;

/**
 * This is a basic contract for defining move deciders for the exhaustive method.
 * 
 * @see BasicVariableExhaustiveSearchDecider
 */
public abstract sealed class AbstractExhaustiveSearchDecider<Solution_>
        implements ExhaustiveSearchPhaseLifecycleListener<Solution_>
        permits BasicVariableExhaustiveSearchDecider {

    private final String logIndentation;
    private final BestSolutionRecaller<Solution_> bestSolutionRecaller;
    private final PhaseTermination<Solution_> termination;
    private final MoveRepository<Solution_> moveRepository;
    private final boolean scoreBounderEnabled;
    private final ScoreBounder<?> scoreBounder;

    protected boolean assertMoveScoreFromScratch = false;
    protected boolean assertExpectedUndoMoveScore = false;

    protected AbstractExhaustiveSearchDecider(String logIndentation, BestSolutionRecaller<Solution_> bestSolutionRecaller,
            PhaseTermination<Solution_> termination, MoveRepository<Solution_> moveRepository,
            boolean scoreBounderEnabled, ScoreBounder<?> scoreBounder) {
        this.bestSolutionRecaller = bestSolutionRecaller;
        this.termination = termination;
        this.logIndentation = logIndentation;
        this.moveRepository = moveRepository;
        this.scoreBounderEnabled = scoreBounderEnabled;
        this.scoreBounder = scoreBounder;
    }

    // ************************************************************************
    // Getter/Setter methods
    // ************************************************************************

    String getLogIndentation() {
        return logIndentation;
    }

    BestSolutionRecaller<Solution_> getBestSolutionRecaller() {
        return bestSolutionRecaller;
    }

    PhaseTermination<Solution_> getTermination() {
        return termination;
    }

    MoveRepository<Solution_> getMoveRepository() {
        return moveRepository;
    }

    public boolean isScoreBounderEnabled() {
        return scoreBounderEnabled;
    }

    @SuppressWarnings("unchecked")
    public <Score_ extends Score<Score_>> ScoreBounder<Score_> getScoreBounder() {
        return (ScoreBounder<Score_>) scoreBounder;
    }

    public void setAssertMoveScoreFromScratch(boolean assertMoveScoreFromScratch) {
        this.assertMoveScoreFromScratch = assertMoveScoreFromScratch;
    }

    public void setAssertExpectedUndoMoveScore(boolean assertExpectedUndoMoveScore) {
        this.assertExpectedUndoMoveScore = assertExpectedUndoMoveScore;
    }

    // ************************************************************************
    // Worker methods
    // ************************************************************************

    public abstract void expandNode(ExhaustiveSearchStepScope<Solution_> stepScope);

    @Override
    public void solvingStarted(SolverScope<Solution_> solverScope) {
        moveRepository.solvingStarted(solverScope);
    }

    @Override
    public void phaseStarted(ExhaustiveSearchPhaseScope<Solution_> phaseScope) {
        moveRepository.phaseStarted(phaseScope);
    }

    @Override
    public void stepStarted(ExhaustiveSearchStepScope<Solution_> stepScope) {
        moveRepository.stepStarted(stepScope);
    }

    @Override
    public void stepEnded(ExhaustiveSearchStepScope<Solution_> stepScope) {
        moveRepository.stepEnded(stepScope);
    }

    @Override
    public void phaseEnded(ExhaustiveSearchPhaseScope<Solution_> phaseScope) {
        moveRepository.phaseEnded(phaseScope);
    }

    @Override
    public void solvingEnded(SolverScope<Solution_> solverScope) {
        moveRepository.solvingEnded(solverScope);
    }

}
