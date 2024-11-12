package ai.timefold.solver.benchmark.impl;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.regex.Pattern;

import ai.timefold.solver.benchmark.api.PlannerBenchmark;
import ai.timefold.solver.benchmark.api.PlannerBenchmarkFactory;
import ai.timefold.solver.benchmark.config.PlannerBenchmarkConfig;
import ai.timefold.solver.benchmark.config.SolverBenchmarkConfig;
import ai.timefold.solver.benchmark.config.report.BenchmarkReportConfig;
import ai.timefold.solver.benchmark.impl.report.BenchmarkReport;
import ai.timefold.solver.benchmark.impl.report.BenchmarkReportFactory;
import ai.timefold.solver.benchmark.impl.result.PlannerBenchmarkResult;
import ai.timefold.solver.core.config.util.ConfigUtils;
import ai.timefold.solver.core.impl.solver.thread.DefaultSolverThreadFactory;

import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @see PlannerBenchmarkFactory
 */
public class DefaultPlannerBenchmarkFactory extends PlannerBenchmarkFactory {

    public static final Pattern VALID_NAME_PATTERN = Pattern.compile("(?U)^[\\w\\d _\\-\\.\\(\\)]+$");
    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultPlannerBenchmarkFactory.class);

    protected final PlannerBenchmarkConfig plannerBenchmarkConfig;

    public DefaultPlannerBenchmarkFactory(PlannerBenchmarkConfig plannerBenchmarkConfig) {
        if (plannerBenchmarkConfig == null) {
            throw new IllegalStateException("The plannerBenchmarkConfig (" + plannerBenchmarkConfig + ") cannot be null.");
        }
        this.plannerBenchmarkConfig = plannerBenchmarkConfig;
    }

    // ************************************************************************
    // Worker methods
    // ************************************************************************

    /**
     * @return never null
     */
    @Override
    public @NonNull PlannerBenchmark buildPlannerBenchmark() {
        return buildPlannerBenchmark(new Object[0]);
    }

    @Override
    @SafeVarargs
    public final @NonNull <Solution_> PlannerBenchmark buildPlannerBenchmark(@NonNull Solution_ @NonNull... problems) {
        validate();
        generateSolverBenchmarkConfigNames();
        List<SolverBenchmarkConfig> effectiveSolverBenchmarkConfigList = buildEffectiveSolverBenchmarkConfigList();
        PlannerBenchmarkResult plannerBenchmarkResult = new PlannerBenchmarkResult();
        plannerBenchmarkResult.setName(plannerBenchmarkConfig.getName());
        plannerBenchmarkResult.setAggregation(false);
        int parallelBenchmarkCount = resolveParallelBenchmarkCount();
        plannerBenchmarkResult.setParallelBenchmarkCount(parallelBenchmarkCount);
        plannerBenchmarkResult.setWarmUpTimeMillisSpentLimit(calculateWarmUpTimeMillisSpentLimit(30L));
        plannerBenchmarkResult.setUnifiedProblemBenchmarkResultList(new ArrayList<>());
        plannerBenchmarkResult.setSolverBenchmarkResultList(new ArrayList<>(effectiveSolverBenchmarkConfigList.size()));
        for (SolverBenchmarkConfig solverBenchmarkConfig : effectiveSolverBenchmarkConfigList) {
            SolverBenchmarkFactory solverBenchmarkFactory = new SolverBenchmarkFactory(solverBenchmarkConfig);
            solverBenchmarkFactory.buildSolverBenchmark(plannerBenchmarkConfig.getClassLoader(), plannerBenchmarkResult,
                    problems);
        }

        BenchmarkReportConfig benchmarkReportConfig_ =
                plannerBenchmarkConfig.getBenchmarkReportConfig() == null ? new BenchmarkReportConfig()
                        : plannerBenchmarkConfig.getBenchmarkReportConfig();
        BenchmarkReport benchmarkReport =
                new BenchmarkReportFactory(benchmarkReportConfig_).buildBenchmarkReport(plannerBenchmarkResult);
        return new DefaultPlannerBenchmark(plannerBenchmarkResult, plannerBenchmarkConfig.getBenchmarkDirectory(),
                buildExecutorService(parallelBenchmarkCount), buildExecutorService(parallelBenchmarkCount), benchmarkReport);
    }

    private ExecutorService buildExecutorService(int parallelBenchmarkCount) {
        return Executors.newFixedThreadPool(parallelBenchmarkCount, getThreadFactory());
    }

    private ThreadFactory getThreadFactory() {
        var threadFactoryClass = plannerBenchmarkConfig.getThreadFactoryClass();
        if (threadFactoryClass != null) {
            return ConfigUtils.newInstance(plannerBenchmarkConfig, "threadFactoryClass", threadFactoryClass);
        } else {
            return new DefaultSolverThreadFactory("BenchmarkThread");
        }
    }

    protected void validate() {
        var name = plannerBenchmarkConfig.getName();
        if (name != null) {
            if (!VALID_NAME_PATTERN.matcher(name).matches()) {
                throw new IllegalStateException(
                        "The plannerBenchmark name (%s) is invalid because it does not follow the nameRegex (%s) which might cause an illegal filename."
                                .formatted(name, VALID_NAME_PATTERN.pattern()));
            }
            if (!name.trim().equals(name)) {
                throw new IllegalStateException(
                        "The plannerBenchmark name (%s) is invalid because it starts or ends with whitespace."
                                .formatted(name));
            }
        }
        if (ConfigUtils.isEmptyCollection(plannerBenchmarkConfig.getSolverBenchmarkBluePrintConfigList())
                && ConfigUtils.isEmptyCollection(plannerBenchmarkConfig.getSolverBenchmarkConfigList())) {
            throw new IllegalArgumentException(
                    "Configure at least 1 <solverBenchmark> (or 1 <solverBenchmarkBluePrint>) in the <plannerBenchmark> configuration.");
        }
    }

    protected void generateSolverBenchmarkConfigNames() {
        if (plannerBenchmarkConfig.getSolverBenchmarkConfigList() != null) {
            Set<String> nameSet = new HashSet<>(plannerBenchmarkConfig.getSolverBenchmarkConfigList().size());
            Set<SolverBenchmarkConfig> noNameBenchmarkConfigSet =
                    new LinkedHashSet<>(plannerBenchmarkConfig.getSolverBenchmarkConfigList().size());
            for (SolverBenchmarkConfig solverBenchmarkConfig : plannerBenchmarkConfig.getSolverBenchmarkConfigList()) {
                if (solverBenchmarkConfig.getName() != null) {
                    boolean unique = nameSet.add(solverBenchmarkConfig.getName());
                    if (!unique) {
                        throw new IllegalStateException("The benchmark name (" + solverBenchmarkConfig.getName()
                                + ") is used in more than 1 benchmark.");
                    }
                } else {
                    noNameBenchmarkConfigSet.add(solverBenchmarkConfig);
                }
            }
            int generatedNameIndex = 0;
            for (SolverBenchmarkConfig solverBenchmarkConfig : noNameBenchmarkConfigSet) {
                String generatedName = "Config_" + generatedNameIndex;
                while (nameSet.contains(generatedName)) {
                    generatedNameIndex++;
                    generatedName = "Config_" + generatedNameIndex;
                }
                solverBenchmarkConfig.setName(generatedName);
                generatedNameIndex++;
            }
        }
    }

    protected List<SolverBenchmarkConfig> buildEffectiveSolverBenchmarkConfigList() {
        var effectiveSolverBenchmarkConfigList = new ArrayList<SolverBenchmarkConfig>(0);
        var solverBenchmarkConfigList = plannerBenchmarkConfig.getSolverBenchmarkConfigList();
        if (solverBenchmarkConfigList != null) {
            effectiveSolverBenchmarkConfigList.addAll(solverBenchmarkConfigList);
        }
        var solverBenchmarkBluePrintConfigList = plannerBenchmarkConfig.getSolverBenchmarkBluePrintConfigList();
        if (solverBenchmarkBluePrintConfigList != null) {
            for (var solverBenchmarkBluePrintConfig : solverBenchmarkBluePrintConfigList) {
                effectiveSolverBenchmarkConfigList.addAll(solverBenchmarkBluePrintConfig.buildSolverBenchmarkConfigList());
            }
        }
        var inheritedSolverBenchmarkConfig = plannerBenchmarkConfig.getInheritedSolverBenchmarkConfig();
        if (inheritedSolverBenchmarkConfig != null) {
            for (var solverBenchmarkConfig : effectiveSolverBenchmarkConfigList) {
                // Side effect: changes the unmarshalled solverBenchmarkConfig
                solverBenchmarkConfig.inherit(inheritedSolverBenchmarkConfig);
            }
        }
        return effectiveSolverBenchmarkConfigList;
    }

    protected int resolveParallelBenchmarkCount() {
        var availableProcessorCount = Runtime.getRuntime().availableProcessors();
        var resolvedParallelBenchmarkCount = actuallyResolverParallelBenchmarkCount(availableProcessorCount);
        if (resolvedParallelBenchmarkCount < 1) {
            throw new IllegalArgumentException(
                    "The parallelBenchmarkCount (%s) resulted in a resolvedParallelBenchmarkCount (%d) that is lower than 1."
                            .formatted(plannerBenchmarkConfig.getParallelBenchmarkCount(), resolvedParallelBenchmarkCount));
        }
        if (resolvedParallelBenchmarkCount > availableProcessorCount) {
            LOGGER.warn("Because the resolvedParallelBenchmarkCount ({}) is higher "
                    + "than the availableProcessorCount ({}), it is reduced to "
                    + "availableProcessorCount.", resolvedParallelBenchmarkCount, availableProcessorCount);
            resolvedParallelBenchmarkCount = availableProcessorCount;
        }
        return resolvedParallelBenchmarkCount;
    }

    private int actuallyResolverParallelBenchmarkCount(int availableProcessorCount) {
        var parallelBenchmarkCount = plannerBenchmarkConfig.getParallelBenchmarkCount();
        if (parallelBenchmarkCount == null) {
            return 1;
        } else if (parallelBenchmarkCount.equals(PlannerBenchmarkConfig.PARALLEL_BENCHMARK_COUNT_AUTO)) {
            return resolveParallelBenchmarkCountAutomatically(availableProcessorCount);
        } else {
            return ConfigUtils.resolvePoolSize("parallelBenchmarkCount", parallelBenchmarkCount,
                    PlannerBenchmarkConfig.PARALLEL_BENCHMARK_COUNT_AUTO);
        }
    }

    protected int resolveParallelBenchmarkCountAutomatically(int availableProcessorCount) {
        // Tweaked based on experience
        if (availableProcessorCount <= 2) {
            return 1;
        } else if (availableProcessorCount <= 4) {
            return 2;
        } else {
            return (availableProcessorCount / 2) + 1;
        }
    }

    protected long calculateWarmUpTimeMillisSpentLimit(long defaultWarmUpTimeMillisSpentLimit) {
        if (plannerBenchmarkConfig.getWarmUpMillisecondsSpentLimit() == null
                && plannerBenchmarkConfig.getWarmUpSecondsSpentLimit() == null
                && plannerBenchmarkConfig.getWarmUpMinutesSpentLimit() == null
                && plannerBenchmarkConfig.getWarmUpHoursSpentLimit() == null
                && plannerBenchmarkConfig.getWarmUpDaysSpentLimit() == null) {
            return defaultWarmUpTimeMillisSpentLimit;
        }
        var warmUpTimeMillisSpentLimit =
                resolveLimit(plannerBenchmarkConfig.getWarmUpMillisecondsSpentLimit(), "warmUpMillisecondsSpentLimit");
        warmUpTimeMillisSpentLimit +=
                resolveLimit(plannerBenchmarkConfig.getWarmUpSecondsSpentLimit(), "warmUpSecondsSpentLimit") * 1_000L;
        warmUpTimeMillisSpentLimit +=
                resolveLimit(plannerBenchmarkConfig.getWarmUpMinutesSpentLimit(), "warmUpMinutesSpentLimit") * 60_000L;
        warmUpTimeMillisSpentLimit +=
                resolveLimit(plannerBenchmarkConfig.getWarmUpHoursSpentLimit(), "warmUpHoursSpentLimit") * 3_600_000L;
        return warmUpTimeMillisSpentLimit
                + resolveLimit(plannerBenchmarkConfig.getWarmUpDaysSpentLimit(), "warmUpDaysSpentLimit") * 86_400_000L;
    }

    private static long resolveLimit(Long limit, String limitName) {
        if (limit == null) {
            return 0L;
        } else if (limit < 0L) {
            throw new IllegalArgumentException("The %s (%d) cannot be negative.".formatted(limitName, limit));
        }
        return limit;
    }

}
