package ai.timefold.solver.benchmark.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import ai.timefold.solver.benchmark.config.ProblemBenchmarksConfig;
import ai.timefold.solver.benchmark.config.SolverBenchmarkConfig;
import ai.timefold.solver.benchmark.config.statistic.ProblemStatisticType;
import ai.timefold.solver.benchmark.config.statistic.SingleStatisticType;
import ai.timefold.solver.benchmark.impl.result.PlannerBenchmarkResult;
import ai.timefold.solver.benchmark.impl.result.SolverBenchmarkResult;
import ai.timefold.solver.core.config.solver.SolverConfig;
import ai.timefold.solver.core.config.solver.monitoring.MonitoringConfig;
import ai.timefold.solver.core.config.solver.monitoring.SolverMetric;
import ai.timefold.solver.core.config.util.ConfigUtils;
import ai.timefold.solver.core.impl.solver.DefaultSolverFactory;

public class SolverBenchmarkFactory {
    private final SolverBenchmarkConfig config;

    public SolverBenchmarkFactory(SolverBenchmarkConfig config) {
        this.config = config;
    }

    public <Solution_> void buildSolverBenchmark(ClassLoader classLoader, PlannerBenchmarkResult plannerBenchmark,
            Solution_[] extraProblems) {
        validate();
        var solverBenchmarkResult = new SolverBenchmarkResult(plannerBenchmark);
        solverBenchmarkResult.setName(config.getName());
        solverBenchmarkResult.setSubSingleCount(ConfigUtils.inheritOverwritableProperty(config.getSubSingleCount(), 1));
        var solverConfig = Objects.requireNonNullElseGet(config.getSolverConfig(), SolverConfig::new);
        if (solverConfig.getClassLoader() == null) {
            solverConfig.setClassLoader(classLoader);
        }
        var monitoringConfig = solverConfig.getMonitoringConfig();
        var monitoringSolverMetricList =
                monitoringConfig == null ? Collections.<SolverMetric> emptyList() : monitoringConfig.getSolverMetricList();
        if (monitoringConfig != null && monitoringSolverMetricList != null && !monitoringSolverMetricList.isEmpty()) {
            throw new IllegalArgumentException("The solverBenchmarkConfig (%s) has a %s (%s) with a non-empty %s (%s)."
                    .formatted(config, SolverConfig.class.getSimpleName(), solverConfig, MonitoringConfig.class.getSimpleName(),
                            monitoringConfig));
        }
        var solverMetricList = getSolverMetrics(config.getProblemBenchmarksConfig());
        solverBenchmarkResult.setSolverConfig(
                solverConfig.copyConfig().withMonitoringConfig(new MonitoringConfig().withSolverMetricList(solverMetricList)));
        var defaultSolverFactory = new DefaultSolverFactory<Solution_>(solverConfig);
        var solutionDescriptor = defaultSolverFactory.getSolutionDescriptor();
        for (var extraProblem : extraProblems) {
            if (!solutionDescriptor.getSolutionClass().isInstance(extraProblem)) {
                throw new IllegalArgumentException(
                        "The solverBenchmark name (%s) for solution class (%s) cannot solve a problem (%s) of class (%s)."
                                .formatted(config.getName(), solutionDescriptor.getSolutionClass(), extraProblem,
                                        extraProblem == null ? null : extraProblem.getClass()));
            }
        }
        solverBenchmarkResult.setScoreDefinition(solutionDescriptor.getScoreDefinition());
        solverBenchmarkResult.setSingleBenchmarkResultList(new ArrayList<>());
        var problemBenchmarksConfig =
                Objects.requireNonNullElseGet(config.getProblemBenchmarksConfig(), ProblemBenchmarksConfig::new);
        plannerBenchmark.getSolverBenchmarkResultList().add(solverBenchmarkResult);
        var problemBenchmarksFactory = new ProblemBenchmarksFactory(problemBenchmarksConfig);
        problemBenchmarksFactory.buildProblemBenchmarkList(solverBenchmarkResult, extraProblems);
    }

    protected void validate() {
        var configName = config.getName();
        if (configName == null || !DefaultPlannerBenchmarkFactory.VALID_NAME_PATTERN.matcher(configName).matches()) {
            throw new IllegalStateException(
                    "The solverBenchmark name (%s) is invalid because it does not follow the nameRegex (%s) which might cause an illegal filename."
                            .formatted(configName, DefaultPlannerBenchmarkFactory.VALID_NAME_PATTERN.pattern()));
        }
        if (!configName.trim().equals(configName)) {
            throw new IllegalStateException(
                    "The solverBenchmark name (%s) is invalid because it starts or ends with whitespace."
                            .formatted(configName));
        }
        var subSingleCount = config.getSubSingleCount();
        if (subSingleCount != null && subSingleCount < 1) {
            throw new IllegalStateException(
                    "The solverBenchmark name (%s) is invalid because the subSingleCount (%d) must be greater than 1."
                            .formatted(configName, subSingleCount));
        }
    }

    protected List<SolverMetric> getSolverMetrics(ProblemBenchmarksConfig config) {
        List<SolverMetric> out = new ArrayList<>();
        for (ProblemStatisticType problemStatisticType : Optional.ofNullable(config)
                .map(ProblemBenchmarksConfig::determineProblemStatisticTypeList)
                .orElseGet(ProblemStatisticType::defaultList)) {
            if (problemStatisticType == ProblemStatisticType.SCORE_CALCULATION_SPEED) {
                out.add(SolverMetric.SCORE_CALCULATION_COUNT);
            } else if (problemStatisticType == ProblemStatisticType.MOVE_EVALUATION_SPEED) {
                out.add(SolverMetric.MOVE_EVALUATION_COUNT);
            } else {
                out.add(SolverMetric.valueOf(problemStatisticType.name()));
            }
        }
        for (SingleStatisticType singleStatisticType : Optional.ofNullable(config)
                .map(ProblemBenchmarksConfig::determineSingleStatisticTypeList)
                .orElseGet(Collections::emptyList)) {
            out.add(SolverMetric.valueOf(singleStatisticType.name()));
        }
        return out;
    }
}
