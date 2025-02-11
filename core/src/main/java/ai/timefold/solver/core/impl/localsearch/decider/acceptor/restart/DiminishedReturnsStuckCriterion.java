package ai.timefold.solver.core.impl.localsearch.decider.acceptor.restart;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.impl.localsearch.scope.LocalSearchMoveScope;
import ai.timefold.solver.core.impl.localsearch.scope.LocalSearchPhaseScope;
import ai.timefold.solver.core.impl.localsearch.scope.LocalSearchStepScope;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;
import ai.timefold.solver.core.impl.solver.termination.DiminishedReturnsTermination;

public class DiminishedReturnsStuckCriterion<Solution_, Score_ extends Score<Score_>>
        extends AbstractGeometricStuckCriterion<Solution_> {
    protected static final long TIME_WINDOW_MILLIS = 60_000;
    private static final double MINIMAL_IMPROVEMENT = 0.0001;

    private DiminishedReturnsTermination<Solution_, Score_> diminishedReturnsCriterion;

    private boolean triggered;
    private Score_ currentBestScore;

    public DiminishedReturnsStuckCriterion() {
        this(new DiminishedReturnsTermination<>(TIME_WINDOW_MILLIS, MINIMAL_IMPROVEMENT));
    }

    protected DiminishedReturnsStuckCriterion(DiminishedReturnsTermination<Solution_, Score_> diminishedReturnsCriterion) {
        super(TIME_WINDOW_MILLIS);
        this.diminishedReturnsCriterion = diminishedReturnsCriterion;
    }

    @Override
    @SuppressWarnings("unchecked")
    boolean evaluateCriterion(LocalSearchMoveScope<Solution_> moveScope) {
        var bestScore = moveScope.getStepScope().getPhaseScope().getBestScore();
        if (moveScope.getScore().compareTo(bestScore) > 0) {
            bestScore = moveScope.getScore();
        }
        triggered = diminishedReturnsCriterion.isTerminated(System.nanoTime(), (Score_) bestScore);
        return triggered;
    }

    @Override
    public void stepStarted(LocalSearchStepScope<Solution_> stepScope) {
        currentBestScore = stepScope.getPhaseScope().getBestScore();
        if (triggered) {
            // We need to recreate the termination criterion as the time window has changed
            diminishedReturnsCriterion = new DiminishedReturnsTermination<>(nextRestart, MINIMAL_IMPROVEMENT);
            diminishedReturnsCriterion.start(System.nanoTime(), stepScope.getPhaseScope().getBestScore());
            triggered = false;
        }
    }

    @Override
    public void stepEnded(LocalSearchStepScope<Solution_> stepScope) {
        diminishedReturnsCriterion.stepEnded(stepScope);
        if (currentBestScore.compareTo(stepScope.getPhaseScope().getBestScore()) < 0 && nextRestart > TIME_WINDOW_MILLIS) {
            // If the solution has been improved after a restart,
            // we reset the criterion and restart the evaluation of the metric
            super.solvingStarted(stepScope.getPhaseScope().getSolverScope());
            diminishedReturnsCriterion = new DiminishedReturnsTermination<>(nextRestart, MINIMAL_IMPROVEMENT);
            diminishedReturnsCriterion.start(System.nanoTime(), stepScope.getPhaseScope().getBestScore());
            logger.info("Stuck criterion reset, next restart ({}), previous best score({}), new best score ({})", nextRestart,
                    currentBestScore, stepScope.getPhaseScope().getBestScore());
        }
    }

    @Override
    public void phaseStarted(LocalSearchPhaseScope<Solution_> phaseScope) {
        super.phaseStarted(phaseScope);
        diminishedReturnsCriterion.phaseStarted(phaseScope);
        triggered = false;
    }

    @Override
    public void phaseEnded(LocalSearchPhaseScope<Solution_> phaseScope) {
        super.phaseEnded(phaseScope);
        diminishedReturnsCriterion.phaseEnded(phaseScope);
    }

    @Override
    public void solvingStarted(SolverScope<Solution_> solverScope) {
        super.solvingStarted(solverScope);
        diminishedReturnsCriterion.solvingStarted(solverScope);
    }

    @Override
    public void solvingEnded(SolverScope<Solution_> solverScope) {
        super.solvingEnded(solverScope);
        diminishedReturnsCriterion.solvingEnded(solverScope);
    }
}
