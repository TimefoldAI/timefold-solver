package ai.timefold.solver.benchmark.impl;

import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.Callable;

import ai.timefold.solver.benchmark.impl.result.SubSingleBenchmarkResult;
import ai.timefold.solver.benchmark.impl.statistic.StatisticRegistry;
import ai.timefold.solver.core.api.solver.SolutionManager;
import ai.timefold.solver.core.api.solver.SolutionUpdatePolicy;
import ai.timefold.solver.core.config.solver.SolverConfig;
import ai.timefold.solver.core.impl.solver.DefaultSolver;
import ai.timefold.solver.core.impl.solver.DefaultSolverFactory;

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
        var runtime = Runtime.getRuntime();
        var singleBenchmarkResult = subSingleBenchmarkResult.getSingleBenchmarkResult();
        var problemBenchmarkResult = singleBenchmarkResult.getProblemBenchmarkResult();
        var problem = (Solution_) problemBenchmarkResult.readProblem();
        if (!problemBenchmarkResult.getPlannerBenchmarkResult().hasMultipleParallelBenchmarks()) {
            runtime.gc();
            subSingleBenchmarkResult.setUsedMemoryAfterInputSolution(runtime.totalMemory() - runtime.freeMemory());
        }
        LOGGER.trace("Benchmark problem has been read for subSingleBenchmarkResult ({}).",
                subSingleBenchmarkResult);

        var solverConfig = singleBenchmarkResult.getSolverBenchmarkResult()
                .getSolverConfig();
        if (singleBenchmarkResult.getSubSingleCount() > 1) {
            solverConfig = new SolverConfig(solverConfig);
            solverConfig.offerRandomSeedFromSubSingleIndex(subSingleBenchmarkResult.getSubSingleBenchmarkIndex());
        }
        var subSingleBenchmarkTagMap = new HashMap<String, String>();
        var runId = UUID.randomUUID().toString();
        subSingleBenchmarkTagMap.put("timefold.benchmark.run", runId);
        solverConfig = new SolverConfig(solverConfig);
        randomSeed = solverConfig.getRandomSeed();

        // Defensive copy of solverConfig for every SingleBenchmarkResult to reset Random, tabu lists, ...
        var solverFactory = new DefaultSolverFactory<Solution_>(new SolverConfig(solverConfig));

        // Register metrics
        var statisticRegistry = new StatisticRegistry<Solution_>(solverFactory.getSolutionDescriptor().getScoreDefinition());
        Metrics.addRegistry(statisticRegistry);
        var runTag = Tags.of("timefold.benchmark.run", runId);
        subSingleBenchmarkResult.getEffectiveSubSingleStatisticMap().forEach((statisticType, subSingleStatistic) -> {
            subSingleStatistic.open(statisticRegistry, runTag);
            subSingleStatistic.initPointList();
        });

        var solver = (DefaultSolver<Solution_>) solverFactory.buildSolver();
        solver.setMonitorTagMap(subSingleBenchmarkTagMap);
        solver.addPhaseLifecycleListener(statisticRegistry);
        var solution = solver.solve(problem);

        solver.removePhaseLifecycleListener(statisticRegistry);
        Metrics.removeRegistry(statisticRegistry);
        var timeMillisSpent = solver.getTimeMillisSpent();

        for (var subSingleStatistic : subSingleBenchmarkResult.getEffectiveSubSingleStatisticMap().values()) {
            subSingleStatistic.close(statisticRegistry, runTag);
            subSingleStatistic.hibernatePointList();
        }
        if (!warmUp) {
            var solverScope = solver.getSolverScope();
            var solutionDescriptor = solverScope.getSolutionDescriptor();
            problemBenchmarkResult.registerProblemSizeStatistics(solverScope.getProblemSizeStatistics());
            subSingleBenchmarkResult.setScore(solutionDescriptor.getScore(solution), solverScope.isBestSolutionInitialized());
            subSingleBenchmarkResult.setTimeMillisSpent(timeMillisSpent);
            subSingleBenchmarkResult.setScoreCalculationCount(solverScope.getScoreCalculationCount());
            subSingleBenchmarkResult.setMoveEvaluationCount(solverScope.getMoveEvaluationCount());

            var solutionManager = SolutionManager.create(solverFactory);
            var isConstraintMatchEnabled = solver.getSolverScope().getScoreDirector().getConstraintMatchPolicy()
                    .isEnabled();
            if (isConstraintMatchEnabled) { // Easy calculator fails otherwise.
                var scoreExplanation =
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
