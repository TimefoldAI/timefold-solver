package ai.timefold.solver.core.impl.solver.scope;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
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
import ai.timefold.solver.core.impl.score.director.InnerScoreDirector;
import ai.timefold.solver.core.impl.solver.AbstractSolver;
import ai.timefold.solver.core.impl.solver.change.DefaultProblemChangeDirector;
import ai.timefold.solver.core.impl.solver.termination.Termination;
import ai.timefold.solver.core.impl.solver.thread.ChildThreadType;

import io.micrometer.core.instrument.Tags;

/**
 * @param <Solution_> the solution type, the class with the {@link PlanningSolution} annotation
 */
public class SolverScope<Solution_> {

    // Solution-derived fields have the potential for race conditions.
    private final AtomicReference<ProblemSizeStatistics> problemSizeStatistics = new AtomicReference<>();
    private final AtomicReference<Solution_> bestSolution = new AtomicReference<>();
    private final AtomicReference<Score<?>> bestScore = new AtomicReference<>();
    private final AtomicLong startingSystemTimeMillis = resetAtomicLongTimeMillis(new AtomicLong());
    private final AtomicLong endingSystemTimeMillis = resetAtomicLongTimeMillis(new AtomicLong());

    private Set<SolverMetric> solverMetricSet;
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

    private long childThreadsScoreCalculationCount = 0;
    private long moveEvaluationCount = 0;

    private Score<?> startingInitializedScore;

    private Long bestSolutionTimeMillis;

    /**
     * Used for tracking step score
     */
    private final Map<Tags, List<AtomicReference<Number>>> stepScoreMap = new ConcurrentHashMap<>();

    private static AtomicLong resetAtomicLongTimeMillis(AtomicLong atomicLong) {
        atomicLong.set(-1);
        return atomicLong;
    }

    private static Long readAtomicLongTimeMillis(AtomicLong atomicLong) {
        var value = atomicLong.get();
        return value == -1 ? null : value;
    }

    // ************************************************************************
    // Constructors and simple getters/setters
    // ************************************************************************
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

    public Map<Tags, List<AtomicReference<Number>>> getStepScoreMap() {
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

    public Score calculateScore() {
        return scoreDirector.calculateScore();
    }

    public void assertScoreFromScratch(Solution_ solution) {
        scoreDirector.getScoreDirectorFactory().assertScoreFromScratch(solution);
    }

    public Score getStartingInitializedScore() {
        return startingInitializedScore;
    }

    public void setStartingInitializedScore(Score startingInitializedScore) {
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

    public long getMoveEvaluationSpeed() {
        long timeMillisSpent = getTimeMillisSpent();
        return getSpeed(getMoveEvaluationCount(), timeMillisSpent);
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

    public Score getBestScore() {
        return bestScore.get();
    }

    public void setBestScore(Score bestScore) {
        this.bestScore.set(bestScore);
    }

    public Long getBestSolutionTimeMillis() {
        return bestSolutionTimeMillis;
    }

    public void setBestSolutionTimeMillis(Long bestSolutionTimeMillis) {
        this.bestSolutionTimeMillis = bestSolutionTimeMillis;
    }

    // ************************************************************************
    // Calculated methods
    // ************************************************************************

    public boolean isMetricEnabled(SolverMetric solverMetric) {
        return solverMetricSet.contains(solverMetric);
    }

    public void startingNow() {
        startingSystemTimeMillis.set(System.currentTimeMillis());
        resetAtomicLongTimeMillis(endingSystemTimeMillis);
    }

    public Long getBestSolutionTimeMillisSpent() {
        return getBestSolutionTimeMillis() - getStartingSystemTimeMillis();
    }

    public void endingNow() {
        endingSystemTimeMillis.set(System.currentTimeMillis());
    }

    public boolean isBestSolutionInitialized() {
        return getBestScore().isSolutionInitialized();
    }

    public long calculateTimeMillisSpentUpToNow() {
        var now = System.currentTimeMillis();
        return now - getStartingSystemTimeMillis();
    }

    public long getTimeMillisSpent() {
        var startingMillis = getStartingSystemTimeMillis();
        if (startingMillis == null) { // The solver hasn't started yet.
            return 0L;
        }
        var endingMillis = getEndingSystemTimeMillis();
        if (endingMillis == null) { // The solver hasn't ended yet.
            endingMillis = System.currentTimeMillis();
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

    public static long getSpeed(long scoreCalculationCount, long timeMillisSpent) {
        // Avoid divide by zero exception on a fast CPU
        return scoreCalculationCount * 1000L / (timeMillisSpent == 0L ? 1L : timeMillisSpent);
    }

    public void setWorkingSolutionFromBestSolution() {
        // The workingSolution must never be the same instance as the bestSolution.
        scoreDirector.setWorkingSolution(scoreDirector.cloneSolution(getBestSolution()));
    }

    public SolverScope<Solution_> createChildThreadSolverScope(ChildThreadType childThreadType) {
        SolverScope<Solution_> childThreadSolverScope = new SolverScope<>();
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
     * Needs to be called <b>before</b> {@link Termination#isPhaseTerminated(AbstractPhaseScope)},
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

}
