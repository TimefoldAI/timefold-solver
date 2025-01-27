package ai.timefold.solver.core.impl.localsearch.decider.reconfiguration;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicReference;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.api.solver.ProblemSizeStatistics;
import ai.timefold.solver.core.config.solver.monitoring.SolverMetric;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.impl.localsearch.decider.LocalSearchDecider;
import ai.timefold.solver.core.impl.score.definition.ScoreDefinition;
import ai.timefold.solver.core.impl.score.director.InnerScoreDirector;
import ai.timefold.solver.core.impl.solver.AbstractSolver;
import ai.timefold.solver.core.impl.solver.change.DefaultProblemChangeDirector;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;
import ai.timefold.solver.core.impl.solver.thread.ChildThreadType;

import io.micrometer.core.instrument.Tags;

public class AdaptedSolverScope<Solution_> extends SolverScope<Solution_> {
    private final SolverScope<Solution_> solverScope;
    private final LocalSearchDecider<Solution_> decider;

    public AdaptedSolverScope(SolverScope<Solution_> solverScope, LocalSearchDecider<Solution_> decider) {
        this.solverScope = solverScope;
        this.decider = decider;
    }

    protected LocalSearchDecider<Solution_> getDecider() {
        return decider;
    }

    @Override
    public AbstractSolver<Solution_> getSolver() {
        return solverScope.getSolver();
    }

    @Override
    public void setSolver(AbstractSolver<Solution_> solver) {
        solverScope.setSolver(solver);
    }

    @Override
    public DefaultProblemChangeDirector<Solution_> getProblemChangeDirector() {
        return solverScope.getProblemChangeDirector();
    }

    @Override
    public void setProblemChangeDirector(DefaultProblemChangeDirector<Solution_> problemChangeDirector) {
        solverScope.setProblemChangeDirector(problemChangeDirector);
    }

    @Override
    public Tags getMonitoringTags() {
        return solverScope.getMonitoringTags();
    }

    @Override
    public void setMonitoringTags(Tags monitoringTags) {
        solverScope.setMonitoringTags(monitoringTags);
    }

    @Override
    public Map<Tags, List<AtomicReference<Number>>> getStepScoreMap() {
        return solverScope.getStepScoreMap();
    }

    @Override
    public Set<SolverMetric> getSolverMetricSet() {
        return solverScope.getSolverMetricSet();
    }

    @Override
    public void setSolverMetricSet(EnumSet<SolverMetric> solverMetricSet) {
        solverScope.setSolverMetricSet(solverMetricSet);
    }

    @Override
    public int getStartingSolverCount() {
        return solverScope.getStartingSolverCount();
    }

    @Override
    public void setStartingSolverCount(int startingSolverCount) {
        solverScope.setStartingSolverCount(startingSolverCount);
    }

    @Override
    public Random getWorkingRandom() {
        return solverScope.getWorkingRandom();
    }

    @Override
    public void setWorkingRandom(Random workingRandom) {
        solverScope.setWorkingRandom(workingRandom);
    }

    @Override
    public <Score_ extends Score<Score_>> InnerScoreDirector<Solution_, Score_> getScoreDirector() {
        return solverScope.getScoreDirector();
    }

    @Override
    public void setScoreDirector(InnerScoreDirector<Solution_, ?> scoreDirector) {
        solverScope.setScoreDirector(scoreDirector);
    }

    @Override
    public void setRunnableThreadSemaphore(Semaphore runnableThreadSemaphore) {
        solverScope.setRunnableThreadSemaphore(runnableThreadSemaphore);
    }

    @Override
    public Long getStartingSystemTimeMillis() {
        return solverScope.getStartingSystemTimeMillis();
    }

    @Override
    public Long getEndingSystemTimeMillis() {
        return solverScope.getEndingSystemTimeMillis();
    }

    @Override
    public SolutionDescriptor<Solution_> getSolutionDescriptor() {
        return solverScope.getSolutionDescriptor();
    }

    @Override
    public ScoreDefinition getScoreDefinition() {
        return solverScope.getScoreDefinition();
    }

    @Override
    public Solution_ getWorkingSolution() {
        return solverScope.getWorkingSolution();
    }

    @Override
    public int getWorkingEntityCount() {
        return solverScope.getWorkingEntityCount();
    }

    @Override
    public Score calculateScore() {
        return solverScope.calculateScore();
    }

    @Override
    public void assertScoreFromScratch(Solution_ solution) {
        solverScope.assertScoreFromScratch(solution);
    }

    @Override
    public Score getStartingInitializedScore() {
        return solverScope.getStartingInitializedScore();
    }

    @Override
    public void setStartingInitializedScore(Score startingInitializedScore) {
        solverScope.setStartingInitializedScore(startingInitializedScore);
    }

    @Override
    public void addChildThreadsScoreCalculationCount(long addition) {
        solverScope.addChildThreadsScoreCalculationCount(addition);
    }

    @Override
    public long getScoreCalculationCount() {
        return solverScope.getScoreCalculationCount();
    }

    @Override
    public void addMoveEvaluationCount(long addition) {
        solverScope.addMoveEvaluationCount(addition);
    }

    @Override
    public void addChildThreadsMoveEvaluationCount(long addition) {
        solverScope.addChildThreadsMoveEvaluationCount(addition);
    }

    @Override
    public long getMoveEvaluationCount() {
        return solverScope.getMoveEvaluationCount();
    }

    @Override
    public Solution_ getBestSolution() {
        return solverScope.getBestSolution();
    }

    @Override
    public void setBestSolution(Solution_ bestSolution) {
        solverScope.setBestSolution(bestSolution);
    }

    @Override
    public Score getBestScore() {
        return solverScope.getBestScore();
    }

    @Override
    public void setBestScore(Score bestScore) {
        solverScope.setBestScore(bestScore);
    }

    @Override
    public Long getBestSolutionTimeMillis() {
        return solverScope.getBestSolutionTimeMillis();
    }

    @Override
    public void setBestSolutionTimeMillis(Long bestSolutionTimeMillis) {
        solverScope.setBestSolutionTimeMillis(bestSolutionTimeMillis);
    }

    @Override
    public Set<String> getMoveCountTypes() {
        return solverScope.getMoveCountTypes();
    }

    @Override
    public Map<String, Long> getMoveEvaluationCountPerType() {
        return solverScope.getMoveEvaluationCountPerType();
    }

    @Override
    public boolean isMetricEnabled(SolverMetric solverMetric) {
        return solverScope.isMetricEnabled(solverMetric);
    }

    @Override
    public void startingNow() {
        solverScope.startingNow();
    }

    @Override
    public Long getBestSolutionTimeMillisSpent() {
        return solverScope.getBestSolutionTimeMillisSpent();
    }

    @Override
    public void endingNow() {
        solverScope.endingNow();
    }

    @Override
    public boolean isBestSolutionInitialized() {
        return solverScope.isBestSolutionInitialized();
    }

    @Override
    public long calculateTimeMillisSpentUpToNow() {
        return solverScope.calculateTimeMillisSpentUpToNow();
    }

    @Override
    public long getTimeMillisSpent() {
        return solverScope.getTimeMillisSpent();
    }

    @Override
    public ProblemSizeStatistics getProblemSizeStatistics() {
        return solverScope.getProblemSizeStatistics();
    }

    @Override
    public void setProblemSizeStatistics(ProblemSizeStatistics problemSizeStatistics) {
        solverScope.setProblemSizeStatistics(problemSizeStatistics);
    }

    @Override
    public long getScoreCalculationSpeed() {
        return solverScope.getScoreCalculationSpeed();
    }

    @Override
    public long getMoveEvaluationSpeed() {
        return solverScope.getMoveEvaluationSpeed();
    }

    @Override
    public void setWorkingSolutionFromBestSolution() {
        solverScope.setWorkingSolutionFromBestSolution();
    }

    @Override
    public SolverScope<Solution_> createChildThreadSolverScope(ChildThreadType childThreadType) {
        return solverScope.createChildThreadSolverScope(childThreadType);
    }

    @Override
    public void initializeYielding() {
        solverScope.initializeYielding();
    }

    @Override
    public void checkYielding() {
        solverScope.checkYielding();
    }

    @Override
    public void destroyYielding() {
        solverScope.destroyYielding();
    }

    @Override
    public void addMoveEvaluationCountPerType(String moveType, long count) {
        solverScope.addMoveEvaluationCountPerType(moveType, count);
    }
}
