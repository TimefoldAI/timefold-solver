package ai.timefold.solver.core.impl.solver.scope;

import static ai.timefold.solver.core.impl.util.MathUtils.getSpeed;

import java.time.Clock;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.api.solver.ProblemSizeStatistics;
import ai.timefold.solver.core.api.solver.Solver;
import ai.timefold.solver.core.config.solver.monitoring.SolverMetric;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.impl.phase.scope.AbstractPhaseScope;
import ai.timefold.solver.core.impl.score.definition.ScoreDefinition;
import ai.timefold.solver.core.impl.score.director.InnerScore;
import ai.timefold.solver.core.impl.score.director.InnerScoreDirector;
import ai.timefold.solver.core.impl.solver.AbstractSolver;
import ai.timefold.solver.core.impl.solver.change.DefaultProblemChangeDirector;
import ai.timefold.solver.core.impl.solver.monitoring.ScoreLevels;
import ai.timefold.solver.core.impl.solver.termination.PhaseTermination;
import ai.timefold.solver.core.impl.solver.thread.ChildThreadType;

import io.micrometer.core.instrument.Tags;

/**
 * @param <Solution_> the solution type, the class with the {@link PlanningSolution} annotation
 */
public class SolverScope<Solution_> {

    private final Clock clock;

    // Solution-derived fields have the potential for race conditions.
    private final AtomicReference<ProblemSizeStatistics> problemSizeStatistics = new AtomicReference<>();
    private final AtomicReference<Solution_> bestSolution = new AtomicReference<>();
    private final AtomicReference<InnerScore<?>> bestScore = new AtomicReference<>();
    private final AtomicLong startingSystemTimeMillis = resetAtomicLongTimeMillis(new AtomicLong());
    private final AtomicLong endingSystemTimeMillis = resetAtomicLongTimeMillis(new AtomicLong());

    private Set<SolverMetric> solverMetricSet = Collections.emptySet();
    private Tags monitoringTags;
    private int startingSolverCount;
    private Random workingRandom;
    private InnerScoreDirector<Solution_, ?> scoreDirector;
    private AbstractSolver<Solution_> solver;
    private DefaultProblemChangeDirector<Solution_> problemChangeDirector;
    /**
     * Used for capping CPU power usage in multithreaded scenarios.
     */
    private Semaphore runnableThreadSemaphore = null;

    private long childThreadsScoreCalculationCount = 0L;

    private long moveEvaluationCount = 0L;

    private Score<?> startingInitializedScore;

    private Long bestSolutionTimeMillis;

    /**
     * Used for tracking step score
     */
    private final Map<Tags, ScoreLevels> stepScoreMap = new ConcurrentHashMap<>();

    /**
     * Used for tracking move count per move type
     */
    private final Map<String, Long> moveEvaluationCountPerTypeMap = new ConcurrentHashMap<>();

    private static AtomicLong resetAtomicLongTimeMillis(AtomicLong atomicLong) {
        atomicLong.set(-1);
        return atomicLong;
    }

    private static Long readAtomicLongTimeMillis(AtomicLong atomicLong) {
        var value = atomicLong.get();
        return value == -1 ? null : value;
    }

    public SolverScope() {
        this.clock = Clock.systemDefaultZone();
    }

    public SolverScope(Clock clock) {
        this.clock = Objects.requireNonNull(clock);
    }

    public Clock getClock() {
        return clock;
    }

    public AbstractSolver<Solution_> getSolver() {
        return solver;
    }

    public void setSolver(AbstractSolver<Solution_> solver) {
        this.solver = solver;
    }

    public DefaultProblemChangeDirector<Solution_> getProblemChangeDirector() {
        return problemChangeDirector;
    }

    public void setProblemChangeDirector(DefaultProblemChangeDirector<Solution_> problemChangeDirector) {
        this.problemChangeDirector = problemChangeDirector;
    }

    public Tags getMonitoringTags() {
        return monitoringTags;
    }

    public void setMonitoringTags(Tags monitoringTags) {
        this.monitoringTags = monitoringTags;
    }

    public Map<Tags, ScoreLevels> getStepScoreMap() {
        return stepScoreMap;
    }

    public Set<SolverMetric> getSolverMetricSet() {
        return solverMetricSet;
    }

    public void setSolverMetricSet(EnumSet<SolverMetric> solverMetricSet) {
        this.solverMetricSet = solverMetricSet;
    }

    public int getStartingSolverCount() {
        return startingSolverCount;
    }

    public void setStartingSolverCount(int startingSolverCount) {
        this.startingSolverCount = startingSolverCount;
    }

    public Random getWorkingRandom() {
        return workingRandom;
    }

    public void setWorkingRandom(Random workingRandom) {
        this.workingRandom = workingRandom;
    }

    @SuppressWarnings("unchecked")
    public <Score_ extends Score<Score_>> InnerScoreDirector<Solution_, Score_> getScoreDirector() {
        return (InnerScoreDirector<Solution_, Score_>) scoreDirector;
    }

    public void setScoreDirector(InnerScoreDirector<Solution_, ?> scoreDirector) {
        this.scoreDirector = scoreDirector;
    }

    public void setRunnableThreadSemaphore(Semaphore runnableThreadSemaphore) {
        this.runnableThreadSemaphore = runnableThreadSemaphore;
    }

    public Long getStartingSystemTimeMillis() {
        return readAtomicLongTimeMillis(startingSystemTimeMillis);
    }

    public Long getEndingSystemTimeMillis() {
        return readAtomicLongTimeMillis(endingSystemTimeMillis);
    }

    public SolutionDescriptor<Solution_> getSolutionDescriptor() {
        return scoreDirector.getSolutionDescriptor();
    }

    public ScoreDefinition getScoreDefinition() {
        return scoreDirector.getScoreDefinition();
    }

    public Solution_ getWorkingSolution() {
        return scoreDirector.getWorkingSolution();
    }

    public int getWorkingEntityCount() {
        return scoreDirector.getWorkingGenuineEntityCount();
    }

    public <Score_ extends Score<Score_>> InnerScore<Score_> calculateScore() {
        return this.<Score_> getScoreDirector().calculateScore();
    }

    public void assertScoreFromScratch(Solution_ solution) {
        scoreDirector.getScoreDirectorFactory().assertScoreFromScratch(solution);
    }

    @SuppressWarnings("unchecked")
    public <Score_ extends Score<Score_>> Score_ getStartingInitializedScore() {
        return (Score_) startingInitializedScore;
    }

    public void setStartingInitializedScore(Score<?> startingInitializedScore) {
        this.startingInitializedScore = startingInitializedScore;
    }

    public void addChildThreadsScoreCalculationCount(long addition) {
        childThreadsScoreCalculationCount += addition;
    }

    public long getScoreCalculationCount() {
        return scoreDirector.getCalculationCount() + childThreadsScoreCalculationCount;
    }

    public void addMoveEvaluationCount(long addition) {
        moveEvaluationCount += addition;
    }

    public long getMoveEvaluationCount() {
        return moveEvaluationCount;
    }

    public Solution_ getBestSolution() {
        return bestSolution.get();
    }

    /**
     * The {@link PlanningSolution best solution} must never be the same instance
     * as the {@link PlanningSolution working solution}, it should be a (un)changed clone.
     *
     * @param bestSolution never null
     */
    public void setBestSolution(Solution_ bestSolution) {
        this.bestSolution.set(bestSolution);
    }

    @SuppressWarnings("unchecked")
    public <Score_ extends Score<Score_>> InnerScore<Score_> getBestScore() {
        return (InnerScore<Score_>) bestScore.get();
    }

    public <Score_ extends Score<Score_>> void setInitializedBestScore(Score_ bestScore) {
        setBestScore(InnerScore.fullyAssigned(bestScore));
    }

    public <Score_ extends Score<Score_>> void setBestScore(InnerScore<Score_> bestScore) {
        this.bestScore.set(bestScore);
    }

    public Long getBestSolutionTimeMillis() {
        return bestSolutionTimeMillis;
    }

    public void setBestSolutionTimeMillis(Long bestSolutionTimeMillis) {
        this.bestSolutionTimeMillis = bestSolutionTimeMillis;
    }

    public Set<String> getMoveCountTypes() {
        return moveEvaluationCountPerTypeMap.keySet();
    }

    public Map<String, Long> getMoveEvaluationCountPerType() {
        return moveEvaluationCountPerTypeMap;
    }

    // ************************************************************************
    // Calculated methods
    // ************************************************************************

    public boolean isMetricEnabled(SolverMetric solverMetric) {
        return solverMetricSet.contains(solverMetric);
    }

    public void startingNow() {
        startingSystemTimeMillis.set(getClock().millis());
        resetAtomicLongTimeMillis(endingSystemTimeMillis);
        this.moveEvaluationCount = 0L;
    }

    public Long getBestSolutionTimeMillisSpent() {
        return getBestSolutionTimeMillis() - getStartingSystemTimeMillis();
    }

    public void endingNow() {
        endingSystemTimeMillis.set(getClock().millis());
    }

    public boolean isBestSolutionInitialized() {
        return getBestScore().isFullyAssigned();
    }

    public long calculateTimeMillisSpentUpToNow() {
        var now = getClock().millis();
        return now - getStartingSystemTimeMillis();
    }

    public long getTimeMillisSpent() {
        var startingMillis = getStartingSystemTimeMillis();
        if (startingMillis == null) { // The solver hasn't started yet.
            return 0L;
        }
        var endingMillis = getEndingSystemTimeMillis();
        if (endingMillis == null) { // The solver hasn't ended yet.
            endingMillis = getClock().millis();
        }
        return endingMillis - startingMillis;
    }

    public ProblemSizeStatistics getProblemSizeStatistics() {
        return problemSizeStatistics.get();
    }

    public void setProblemSizeStatistics(ProblemSizeStatistics problemSizeStatistics) {
        this.problemSizeStatistics.set(problemSizeStatistics);
    }

    /**
     * @return at least 0, per second
     */
    public long getScoreCalculationSpeed() {
        long timeMillisSpent = getTimeMillisSpent();
        return getSpeed(getScoreCalculationCount(), timeMillisSpent);
    }

    /**
     * @return at least 0, per second
     */
    public long getMoveEvaluationSpeed() {
        long timeMillisSpent = getTimeMillisSpent();
        return getSpeed(getMoveEvaluationCount(), timeMillisSpent);
    }

    public void setWorkingSolutionFromBestSolution() {
        // The workingSolution must never be the same instance as the bestSolution.
        scoreDirector.setWorkingSolution(scoreDirector.cloneSolution(getBestSolution()));
    }

    public SolverScope<Solution_> createChildThreadSolverScope(ChildThreadType childThreadType) {
        SolverScope<Solution_> childThreadSolverScope = new SolverScope<>(clock);
        childThreadSolverScope.bestSolution.set(null);
        childThreadSolverScope.bestScore.set(null);
        childThreadSolverScope.monitoringTags = monitoringTags;
        childThreadSolverScope.solverMetricSet = solverMetricSet;
        childThreadSolverScope.startingSolverCount = startingSolverCount;
        // TODO FIXME use RandomFactory
        // Experiments show that this trick to attain reproducibility doesn't break uniform distribution
        childThreadSolverScope.workingRandom = new Random(workingRandom.nextLong());
        childThreadSolverScope.scoreDirector = scoreDirector.createChildThreadScoreDirector(childThreadType);
        childThreadSolverScope.startingSystemTimeMillis.set(startingSystemTimeMillis.get());
        resetAtomicLongTimeMillis(childThreadSolverScope.endingSystemTimeMillis);
        childThreadSolverScope.startingInitializedScore = null;
        childThreadSolverScope.bestSolutionTimeMillis = null;
        return childThreadSolverScope;
    }

    public void initializeYielding() {
        if (runnableThreadSemaphore != null) {
            try {
                runnableThreadSemaphore.acquire();
            } catch (InterruptedException e) {
                // TODO it will take a while before the BasicPlumbingTermination is called
                // The BasicPlumbingTermination will terminate the solver.
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * Similar to {@link Thread#yield()}, but allows capping the number of active solver threads
     * at less than the CPU processor count, so other threads (for example servlet threads that handle REST calls)
     * and other processes (such as SSH) have access to uncontested CPUs and don't suffer any latency.
     * <p>
     * Needs to be called <b>before</b> {@link PhaseTermination#isPhaseTerminated(AbstractPhaseScope)},
     * so the decision to start a new iteration is after any yield waiting time has been consumed
     * (so {@link Solver#terminateEarly()} reacts immediately).
     */
    public void checkYielding() {
        if (runnableThreadSemaphore != null) {
            runnableThreadSemaphore.release();
            try {
                runnableThreadSemaphore.acquire();
            } catch (InterruptedException e) {
                // The BasicPlumbingTermination will terminate the solver.
                Thread.currentThread().interrupt();
            }
        }
    }

    public void destroyYielding() {
        if (runnableThreadSemaphore != null) {
            runnableThreadSemaphore.release();
        }
    }

    public void addMoveEvaluationCountPerType(String moveType, long count) {
        moveEvaluationCountPerTypeMap.compute(moveType, (key, counter) -> {
            if (counter == null) {
                counter = 0L;
            }
            counter += count;
            return counter;
        });
    }
}
