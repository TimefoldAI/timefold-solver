package ai.timefold.solver.core.impl.localsearch.decider.acceptor.stuckcriterion;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.impl.localsearch.scope.LocalSearchMoveScope;
import ai.timefold.solver.core.impl.localsearch.scope.LocalSearchPhaseScope;
import ai.timefold.solver.core.impl.localsearch.scope.LocalSearchStepScope;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;
import ai.timefold.solver.core.impl.solver.termination.UnimprovedTimeMillisSpentTermination;

public class UnimprovedTimeStuckCriterion<Solution_, Score_ extends Score<Score_>>
        extends AbstractGeometricStuckCriterion<Solution_> {
    protected static final long START_TIME_WINDOW_MILLIS = 30_000;
    protected static final long REGULAR_TIME_WINDOW_MILLIS = 300_000;

    private UnimprovedTimeMillisSpentTermination<Solution_> unimprovedTimeStuckCriterion;

    private boolean triggered;
    private Score_ currentBestScore;

    public UnimprovedTimeStuckCriterion() {
        this(new UnimprovedTimeMillisSpentTermination<>(START_TIME_WINDOW_MILLIS));
    }

    protected UnimprovedTimeStuckCriterion(UnimprovedTimeMillisSpentTermination<Solution_> unimprovedTimeStuckCriterion) {
        super(START_TIME_WINDOW_MILLIS);
        this.unimprovedTimeStuckCriterion = unimprovedTimeStuckCriterion;
    }

    @Override
    boolean evaluateCriterion(LocalSearchMoveScope<Solution_> moveScope) {
        triggered = unimprovedTimeStuckCriterion.isPhaseTerminated(moveScope.getStepScope().getPhaseScope());
        return triggered;
    }

    @Override
    public void stepStarted(LocalSearchStepScope<Solution_> stepScope) {
        currentBestScore = stepScope.getPhaseScope().getBestScore();
        if (triggered) {
            // We need to recreate the termination criterion as the time window has changed
            // After the first restart we use a higher time window
            setScalingFactor(REGULAR_TIME_WINDOW_MILLIS);
            unimprovedTimeStuckCriterion = new UnimprovedTimeMillisSpentTermination<>(nextRestart);
            // The criterion must be initialized; otherwise, no restart will occur
            unimprovedTimeStuckCriterion.phaseStarted(stepScope.getPhaseScope());
            triggered = false;
        }
    }

    @Override
    public void stepEnded(LocalSearchStepScope<Solution_> stepScope) {
        unimprovedTimeStuckCriterion.stepEnded(stepScope);
        if (currentBestScore.compareTo(stepScope.getPhaseScope().getBestScore()) < 0
                && nextRestart > START_TIME_WINDOW_MILLIS) {
            // If the solution has been improved after a restart,
            // we reset the criterion and restart the evaluation of the metric
            setScalingFactor(START_TIME_WINDOW_MILLIS);
            super.solvingStarted(stepScope.getPhaseScope().getSolverScope());
            unimprovedTimeStuckCriterion = new UnimprovedTimeMillisSpentTermination<>(nextRestart);
            // The criterion must be initialized; otherwise, no restart will occur
            unimprovedTimeStuckCriterion.phaseStarted(stepScope.getPhaseScope());
            logger.info("Stuck criterion reset, next restart ({}), previous best score({}), new best score ({})", nextRestart,
                    currentBestScore, stepScope.getPhaseScope().getBestScore());
        }
    }

    @Override
    public void phaseStarted(LocalSearchPhaseScope<Solution_> phaseScope) {
        super.phaseStarted(phaseScope);
        unimprovedTimeStuckCriterion.phaseStarted(phaseScope);
        triggered = false;
    }

    @Override
    public void phaseEnded(LocalSearchPhaseScope<Solution_> phaseScope) {
        super.phaseEnded(phaseScope);
        unimprovedTimeStuckCriterion.phaseEnded(phaseScope);
    }

    @Override
    public void solvingStarted(SolverScope<Solution_> solverScope) {
        super.solvingStarted(solverScope);
        unimprovedTimeStuckCriterion.solvingStarted(solverScope);
    }

    @Override
    public void solvingEnded(SolverScope<Solution_> solverScope) {
        super.solvingEnded(solverScope);
        unimprovedTimeStuckCriterion.solvingEnded(solverScope);
    }
}
