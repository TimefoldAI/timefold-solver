package ai.timefold.solver.benchmark.impl;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;

import ai.timefold.solver.benchmark.impl.result.ProblemBenchmarkResult;
import ai.timefold.solver.benchmark.impl.result.SingleBenchmarkResult;
import ai.timefold.solver.benchmark.impl.result.SubSingleBenchmarkResult;
import ai.timefold.solver.benchmark.impl.statistic.StatisticRegistry;
import ai.timefold.solver.benchmark.impl.statistic.SubSingleStatistic;
import ai.timefold.solver.core.api.score.ScoreExplanation;
import ai.timefold.solver.core.api.solver.SolutionManager;
import ai.timefold.solver.core.api.solver.SolutionUpdatePolicy;
import ai.timefold.solver.core.config.solver.SolverConfig;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.impl.solver.DefaultSolver;
import ai.timefold.solver.core.impl.solver.DefaultSolverFactory;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Tags;

public class SubSingleBenchmarkRunner<Solution_> implements Callable<SubSingleBenchmarkRunner<Solution_>> {

    public static final String NAME_MDC = "subSingleBenchmark.name";

    private static final Logger LOGGER = LoggerFactory.getLogger(SubSingleBenchmarkRunner.class);

    private final SubSingleBenchmarkResult subSingleBenchmarkResult;
    private final boolean warmUp;

    private Long randomSeed = null;
    private Throwable failureThrowable = null;

    /**
     * @param subSingleBenchmarkResult never null
     */
    public SubSingleBenchmarkRunner(SubSingleBenchmarkResult subSingleBenchmarkResult, boolean warmUp) {
        this.subSingleBenchmarkResult = subSingleBenchmarkResult;
        this.warmUp = warmUp;
    }

    public SubSingleBenchmarkResult getSubSingleBenchmarkResult() {
        return subSingleBenchmarkResult;
    }

    public Long getRandomSeed() {
        return randomSeed;
    }

    public Throwable getFailureThrowable() {
        return failureThrowable;
    }

    public void setFailureThrowable(Throwable failureThrowable) {
        this.failureThrowable = failureThrowable;
    }

    // ************************************************************************
    // Benchmark methods
    // ************************************************************************

    @Override
    public SubSingleBenchmarkRunner<Solution_> call() {
        MDC.put(NAME_MDC, subSingleBenchmarkResult.getName());
        Runtime runtime = Runtime.getRuntime();
        SingleBenchmarkResult singleBenchmarkResult = subSingleBenchmarkResult.getSingleBenchmarkResult();
        ProblemBenchmarkResult<Solution_> problemBenchmarkResult = singleBenchmarkResult
                .getProblemBenchmarkResult();
        Solution_ problem = problemBenchmarkResult.readProblem();
        if (!problemBenchmarkResult.getPlannerBenchmarkResult().hasMultipleParallelBenchmarks()) {
            runtime.gc();
            subSingleBenchmarkResult.setUsedMemoryAfterInputSolution(runtime.totalMemory() - runtime.freeMemory());
        }
        LOGGER.trace("Benchmark problem has been read for subSingleBenchmarkResult ({}).",
                subSingleBenchmarkResult);

        SolverConfig solverConfig = singleBenchmarkResult.getSolverBenchmarkResult()
                .getSolverConfig();
        if (singleBenchmarkResult.getSubSingleCount() > 1) {
            solverConfig = new SolverConfig(solverConfig);
            solverConfig.offerRandomSeedFromSubSingleIndex(subSingleBenchmarkResult.getSubSingleBenchmarkIndex());
        }
        Map<String, String> subSingleBenchmarkTagMap = new HashMap<>();
        String runId = UUID.randomUUID().toString();
        subSingleBenchmarkTagMap.put("timefold.benchmark.run", runId);
        solverConfig = new SolverConfig(solverConfig);
        randomSeed = solverConfig.getRandomSeed();
        // Defensive copy of solverConfig for every SingleBenchmarkResult to reset Random, tabu lists, ...
        DefaultSolverFactory<Solution_> solverFactory = new DefaultSolverFactory<>(new SolverConfig(solverConfig));
        DefaultSolver<Solution_> solver = (DefaultSolver<Solution_>) solverFactory.buildSolver();
        solver.setMonitorTagMap(subSingleBenchmarkTagMap);
        StatisticRegistry<Solution_> statisticRegistry = new StatisticRegistry<>(solver);
        Metrics.addRegistry(statisticRegistry);
        solver.addPhaseLifecycleListener(statisticRegistry);

        Tags runTag = Tags.of("timefold.benchmark.run", runId);
        for (SubSingleStatistic<Solution_, ?> subSingleStatistic : subSingleBenchmarkResult.getEffectiveSubSingleStatisticMap()
                .values()) {
            subSingleStatistic.open(statisticRegistry, runTag, solver);
            subSingleStatistic.initPointList();
        }
        Solution_ solution = solver.solve(problem);

        solver.removePhaseLifecycleListener(statisticRegistry);
        Metrics.removeRegistry(statisticRegistry);
        long timeMillisSpent = solver.getTimeMillisSpent();

        for (SubSingleStatistic<Solution_, ?> subSingleStatistic : subSingleBenchmarkResult.getEffectiveSubSingleStatisticMap()
                .values()) {
            subSingleStatistic.close(statisticRegistry, runTag, solver);
            subSingleStatistic.hibernatePointList();
        }
        if (!warmUp) {
            SolverScope<Solution_> solverScope = solver.getSolverScope();
            SolutionDescriptor<Solution_> solutionDescriptor = solverScope.getSolutionDescriptor();
            problemBenchmarkResult.registerScale(solutionDescriptor.getEntityCount(solution),
                    solutionDescriptor.getGenuineVariableCount(solution),
                    solutionDescriptor.getMaximumValueCount(solution),
                    solutionDescriptor.getProblemScale(solution));
            subSingleBenchmarkResult.setScore(solutionDescriptor.getScore(solution));
            subSingleBenchmarkResult.setTimeMillisSpent(timeMillisSpent);
            subSingleBenchmarkResult.setScoreCalculationCount(solverScope.getScoreCalculationCount());

            SolutionManager<Solution_, ?> solutionManager = SolutionManager.create(solverFactory);
            boolean isConstraintMatchEnabled = solver.getSolverScope().getScoreDirector().isConstraintMatchEnabled();
            if (isConstraintMatchEnabled) { // Easy calculator fails otherwise.
                ScoreExplanation<Solution_, ?> scoreExplanation =
                        solutionManager.explain(solution, SolutionUpdatePolicy.NO_UPDATE);
                subSingleBenchmarkResult.setScoreExplanationSummary(scoreExplanation.getSummary());
            }

            problemBenchmarkResult.writeSolution(subSingleBenchmarkResult, solution);
        }
        MDC.remove(NAME_MDC);
        return this;
    }

    public String getName() {
        return subSingleBenchmarkResult.getName();
    }

    @Override
    public String toString() {
        return subSingleBenchmarkResult.toString();
    }

}
