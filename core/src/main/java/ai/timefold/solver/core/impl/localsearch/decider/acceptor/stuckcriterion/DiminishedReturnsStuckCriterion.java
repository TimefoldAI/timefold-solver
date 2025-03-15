package ai.timefold.solver.core.impl.localsearch.decider.acceptor.stuckcriterion;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.impl.localsearch.scope.LocalSearchPhaseScope;
import ai.timefold.solver.core.impl.localsearch.scope.LocalSearchStepScope;
import ai.timefold.solver.core.impl.solver.termination.DiminishedReturnsTermination;

public class DiminishedReturnsStuckCriterion<Solution_, Score_ extends Score<Score_>>
        extends AbstractGeometricStuckCriterion<Solution_> {
    protected static final long TIME_WINDOW_MILLIS = 10_000;
    private static final double MINIMAL_IMPROVEMENT = 0.0001;

    private DiminishedReturnsTermination<Solution_, Score_> diminishedReturnsCriterion;

    protected boolean triggered;

    public DiminishedReturnsStuckCriterion() {
        this(new DiminishedReturnsTermination<>(TIME_WINDOW_MILLIS, MINIMAL_IMPROVEMENT));
    }

    protected DiminishedReturnsStuckCriterion(DiminishedReturnsTermination<Solution_, Score_> diminishedReturnsCriterion) {
        super(TIME_WINDOW_MILLIS);
        this.diminishedReturnsCriterion = diminishedReturnsCriterion;
    }

    @Override
    @SuppressWarnings("unchecked")
    boolean evaluateCriterion(LocalSearchStepScope<Solution_> stepScope) {
        var bestScore = stepScope.getPhaseScope().getBestScore();
        if (((Score) stepScope.getScore()).compareTo(bestScore) > 0) {
            bestScore = stepScope.getScore();
        }
        triggered = diminishedReturnsCriterion.isTerminated(System.nanoTime(), (Score_) bestScore);
        return triggered;
    }

    @Override
    public void reset(LocalSearchStepScope<Solution_> stepScope) {
        diminishedReturnsCriterion = new DiminishedReturnsTermination<>(nextRestart, MINIMAL_IMPROVEMENT);
        diminishedReturnsCriterion.start(System.nanoTime(), stepScope.getPhaseScope().getBestScore());
        triggered = false;
    }

    @Override
    public void stepStarted(LocalSearchStepScope<Solution_> stepScope) {
        if (triggered) {
            reset(stepScope);
        }
    }

    @Override
    public void stepEnded(LocalSearchStepScope<Solution_> stepScope) {
        diminishedReturnsCriterion.stepEnded(stepScope);
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
}
