package ai.timefold.solver.core.impl.phase.scope;

import java.util.Random;

import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.config.solver.monitoring.SolverMetric;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.impl.score.director.InnerScore;
import ai.timefold.solver.core.impl.score.director.InnerScoreDirector;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;
import ai.timefold.solver.core.impl.solver.termination.PhaseTermination;
import ai.timefold.solver.core.preview.api.move.Move;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @param <Solution_> the solution type, the class with the {@link PlanningSolution} annotation
 */
public abstract class AbstractPhaseScope<Solution_> {

    protected final transient Logger logger = LoggerFactory.getLogger(getClass());

    protected final SolverScope<Solution_> solverScope;
    protected final int phaseIndex;
    protected final boolean phaseSendingBestSolutionEvents;

    protected Long startingSystemTimeMillis;
    protected Long startingScoreCalculationCount;
    protected Long startingMoveEvaluationCount;
    protected InnerScore<?> startingScore;
    protected Long endingSystemTimeMillis;
    protected Long endingScoreCalculationCount;
    protected Long endingMoveEvaluationCount;
    protected long childThreadsScoreCalculationCount = 0L;

    protected int bestSolutionStepIndex;

    /**
     * The phase termination configuration
     */
    private PhaseTermination<Solution_> termination;

    /**
     * As defined by #AbstractPhaseScope(SolverScope, int, boolean)
     * with the phaseSendingBestSolutionEvents parameter set to true.
     */
    protected AbstractPhaseScope(SolverScope<Solution_> solverScope, int phaseIndex) {
        this(solverScope, phaseIndex, true);
    }

    /**
     *
     * @param solverScope never null
     * @param phaseIndex the index of the phase, >= 0
     * @param phaseSendingBestSolutionEvents set to false if the phase only sends one best solution event at the end,
     *        or none at all;
     *        this is typical for construction heuristics,
     *        whose result only matters when it reached its natural end.
     */
    protected AbstractPhaseScope(SolverScope<Solution_> solverScope, int phaseIndex, boolean phaseSendingBestSolutionEvents) {
        this.solverScope = solverScope;
        this.phaseIndex = phaseIndex;
        this.phaseSendingBestSolutionEvents = phaseSendingBestSolutionEvents;
    }

    public SolverScope<Solution_> getSolverScope() {
        return solverScope;
    }

    public int getPhaseIndex() {
        return phaseIndex;
    }

    public boolean isPhaseSendingBestSolutionEvents() {
        return phaseSendingBestSolutionEvents;
    }

    public Long getStartingSystemTimeMillis() {
        return startingSystemTimeMillis;
    }

    @SuppressWarnings("unchecked")
    public <Score_ extends Score<Score_>> InnerScore<Score_> getStartingScore() {
        return (InnerScore<Score_>) startingScore;
    }

    public Long getEndingSystemTimeMillis() {
        return endingSystemTimeMillis;
    }

    public int getBestSolutionStepIndex() {
        return bestSolutionStepIndex;
    }

    public void setBestSolutionStepIndex(int bestSolutionStepIndex) {
        this.bestSolutionStepIndex = bestSolutionStepIndex;
    }

    public abstract AbstractStepScope<Solution_> getLastCompletedStepScope();

    // ************************************************************************
    // Calculated methods
    // ************************************************************************

    public void reset() {
        bestSolutionStepIndex = -1;
        // solverScope.getBestScore() is null with an uninitialized score
        startingScore = solverScope.getBestScore() == null ? solverScope.calculateScore() : solverScope.getBestScore();
        if (getLastCompletedStepScope().getStepIndex() < 0) {
            getLastCompletedStepScope().setScore(startingScore);
        }
    }

    public void startingNow() {
        startingSystemTimeMillis = getSolverScope().getClock().millis();
        startingScoreCalculationCount = getScoreDirector().getCalculationCount();
        startingMoveEvaluationCount = getSolverScope().getMoveEvaluationCount();
    }

    public void endingNow() {
        endingSystemTimeMillis = getSolverScope().getClock().millis();
        endingScoreCalculationCount = getScoreDirector().getCalculationCount();
        endingMoveEvaluationCount = getSolverScope().getMoveEvaluationCount();
    }

    public SolutionDescriptor<Solution_> getSolutionDescriptor() {
        return solverScope.getSolutionDescriptor();
    }

    public long calculateSolverTimeMillisSpentUpToNow() {
        return solverScope.calculateTimeMillisSpentUpToNow();
    }

    public long calculatePhaseTimeMillisSpentUpToNow() {
        long now = getSolverScope().getClock().millis();
        return now - startingSystemTimeMillis;
    }

    public long getPhaseTimeMillisSpent() {
        return endingSystemTimeMillis - startingSystemTimeMillis;
    }

    public void addChildThreadsScoreCalculationCount(long addition) {
        solverScope.addChildThreadsScoreCalculationCount(addition);
        childThreadsScoreCalculationCount += addition;
    }

    public void addMoveEvaluationCount(Move<Solution_> move, long count) {
        solverScope.addMoveEvaluationCount(1);
        addMoveEvaluationCountPerType(move, count);
    }

    public void addMoveEvaluationCountPerType(Move<Solution_> move, long count) {
        if (solverScope.isMetricEnabled(SolverMetric.MOVE_COUNT_PER_TYPE)) {
            solverScope.addMoveEvaluationCountPerType(move.describe(), count);
        }
    }

    public long getPhaseScoreCalculationCount() {
        return endingScoreCalculationCount - startingScoreCalculationCount + childThreadsScoreCalculationCount;
    }

    public long getPhaseMoveEvaluationCount() {
        var currentMoveEvaluationCount = endingMoveEvaluationCount;
        if (endingMoveEvaluationCount == null) {
            currentMoveEvaluationCount = getSolverScope().getMoveEvaluationCount();
        }
        return currentMoveEvaluationCount - startingMoveEvaluationCount;
    }

    /**
     * @return at least 0, per second
     */
    public long getPhaseScoreCalculationSpeed() {
        return getMetricCalculationSpeed(getPhaseScoreCalculationCount());
    }

    /**
     * @return at least 0, per second
     */
    public long getPhaseMoveEvaluationSpeed() {
        return getMetricCalculationSpeed(getPhaseMoveEvaluationCount());
    }

    private long getMetricCalculationSpeed(long metric) {
        long timeMillisSpent = getPhaseTimeMillisSpent();
        // Avoid divide by zero exception on a fast CPU
        return metric * 1000L / (timeMillisSpent == 0L ? 1L : timeMillisSpent);
    }

    public <Score_ extends Score<Score_>> InnerScoreDirector<Solution_, Score_> getScoreDirector() {
        return solverScope.getScoreDirector();
    }

    public void setTermination(PhaseTermination<Solution_> termination) {
        this.termination = termination;
    }

    public PhaseTermination<Solution_> getTermination() {
        return termination;
    }

    public Solution_ getWorkingSolution() {
        return solverScope.getWorkingSolution();
    }

    public int getWorkingEntityCount() {
        return solverScope.getWorkingEntityCount();
    }

    public <Score_ extends Score<Score_>> InnerScore<Score_> calculateScore() {
        return solverScope.calculateScore();
    }

    public <Score_ extends Score<Score_>> void assertExpectedWorkingScore(InnerScore<Score_> expectedWorkingScore,
            Object completedAction) {
        InnerScoreDirector<Solution_, Score_> innerScoreDirector = getScoreDirector();
        innerScoreDirector.assertExpectedWorkingScore(expectedWorkingScore, completedAction);
    }

    public <Score_ extends Score<Score_>> void assertWorkingScoreFromScratch(InnerScore<Score_> workingScore,
            Object completedAction) {
        InnerScoreDirector<Solution_, Score_> innerScoreDirector = getScoreDirector();
        innerScoreDirector.assertWorkingScoreFromScratch(workingScore, completedAction);
    }

    public <Score_ extends Score<Score_>> void assertPredictedScoreFromScratch(InnerScore<Score_> workingScore,
            Object completedAction) {
        InnerScoreDirector<Solution_, Score_> innerScoreDirector = getScoreDirector();
        innerScoreDirector.assertPredictedScoreFromScratch(workingScore, completedAction);
    }

    public <Score_ extends Score<Score_>> void assertShadowVariablesAreNotStale(InnerScore<Score_> workingScore,
            Object completedAction) {
        InnerScoreDirector<Solution_, Score_> innerScoreDirector = getScoreDirector();
        innerScoreDirector.assertShadowVariablesAreNotStale(workingScore, completedAction);
    }

    public Random getWorkingRandom() {
        return getSolverScope().getWorkingRandom();
    }

    public boolean isBestSolutionInitialized() {
        return solverScope.isBestSolutionInitialized();
    }

    public <Score_ extends Score<Score_>> InnerScore<Score_> getBestScore() {
        return solverScope.getBestScore();
    }

    public long getPhaseBestSolutionTimeMillis() {
        long bestSolutionTimeMillis = solverScope.getBestSolutionTimeMillis();
        // If the termination is explicitly phase configured, previous phases must not affect it
        if (bestSolutionTimeMillis < startingSystemTimeMillis) {
            bestSolutionTimeMillis = startingSystemTimeMillis;
        }
        return bestSolutionTimeMillis;
    }

    public int getNextStepIndex() {
        return getLastCompletedStepScope().getStepIndex() + 1;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "(" + phaseIndex + ")";
    }

}
