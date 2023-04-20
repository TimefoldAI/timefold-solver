package ai.timefold.solver.spring.boot.autoconfigure;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import ai.timefold.solver.benchmark.api.PlannerBenchmarkFactory;
import ai.timefold.solver.benchmark.config.PlannerBenchmarkConfig;
import ai.timefold.solver.benchmark.config.SolverBenchmarkConfig;
import ai.timefold.solver.core.config.constructionheuristic.ConstructionHeuristicPhaseConfig;
import ai.timefold.solver.core.config.phase.PhaseConfig;
import ai.timefold.solver.core.config.solver.SolverConfig;
import ai.timefold.solver.core.config.solver.termination.TerminationConfig;
import ai.timefold.solver.spring.boot.autoconfigure.config.BenchmarkProperties;
import ai.timefold.solver.spring.boot.autoconfigure.config.TimefoldProperties;

import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@AutoConfigureAfter(TimefoldAutoConfiguration.class)
@ConditionalOnClass({ PlannerBenchmarkFactory.class })
@ConditionalOnMissingBean({ PlannerBenchmarkFactory.class })
@EnableConfigurationProperties({ TimefoldProperties.class })
public class TimefoldBenchmarkAutoConfiguration
        implements BeanClassLoaderAware {

    private final ApplicationContext context;
    private final TimefoldProperties optaPlannerProperties;
    private ClassLoader beanClassLoader;

    protected TimefoldBenchmarkAutoConfiguration(ApplicationContext context,
            TimefoldProperties optaPlannerProperties) {
        this.context = context;
        this.optaPlannerProperties = optaPlannerProperties;
    }

    @Bean
    public PlannerBenchmarkConfig plannerBenchmarkConfig(SolverConfig solverConfig) {
        PlannerBenchmarkConfig benchmarkConfig;
        if (optaPlannerProperties.getBenchmark() != null
                && optaPlannerProperties.getBenchmark().getSolverBenchmarkConfigXml() != null) {
            if (beanClassLoader.getResource(optaPlannerProperties.getBenchmark().getSolverBenchmarkConfigXml()) == null) {
                throw new IllegalStateException(
                        "Invalid optaplanner.benchmark.solverBenchmarkConfigXml property ("
                                + optaPlannerProperties.getBenchmark().getSolverBenchmarkConfigXml()
                                + "): that classpath resource does not exist.");
            }
            benchmarkConfig = PlannerBenchmarkConfig
                    .createFromXmlResource(optaPlannerProperties.getBenchmark().getSolverBenchmarkConfigXml(), beanClassLoader);
        } else if (beanClassLoader.getResource(BenchmarkProperties.DEFAULT_SOLVER_BENCHMARK_CONFIG_URL) != null) {
            benchmarkConfig = PlannerBenchmarkConfig.createFromXmlResource(
                    TimefoldProperties.DEFAULT_SOLVER_BENCHMARK_CONFIG_URL, beanClassLoader);
        } else {
            benchmarkConfig = PlannerBenchmarkConfig.createFromSolverConfig(solverConfig);
            benchmarkConfig.setBenchmarkDirectory(new File(BenchmarkProperties.DEFAULT_BENCHMARK_RESULT_DIRECTORY));
        }

        if (optaPlannerProperties.getBenchmark() != null && optaPlannerProperties.getBenchmark().getResultDirectory() != null) {
            benchmarkConfig.setBenchmarkDirectory(new File(optaPlannerProperties.getBenchmark().getResultDirectory()));
        }

        if (benchmarkConfig.getBenchmarkDirectory() == null) {
            benchmarkConfig.setBenchmarkDirectory(new File(BenchmarkProperties.DEFAULT_BENCHMARK_RESULT_DIRECTORY));
        }

        if (optaPlannerProperties.getBenchmark() != null && optaPlannerProperties.getBenchmark().getSolver() != null) {
            TimefoldAutoConfiguration
                    .applyTerminationProperties(benchmarkConfig.getInheritedSolverBenchmarkConfig().getSolverConfig(),
                            optaPlannerProperties.getBenchmark().getSolver().getTermination());
        }

        if (benchmarkConfig.getInheritedSolverBenchmarkConfig().getSolverConfig().getTerminationConfig() == null ||
                !benchmarkConfig.getInheritedSolverBenchmarkConfig().getSolverConfig().getTerminationConfig().isConfigured()) {
            List<SolverBenchmarkConfig> solverBenchmarkConfigList = benchmarkConfig.getSolverBenchmarkConfigList();
            List<String> unconfiguredTerminationSolverBenchmarkList = new ArrayList<>();
            if (solverBenchmarkConfigList == null) {
                throw new IllegalStateException("At least one of the properties " +
                        "optaplanner.benchmark.solver.termination.spent-limit, " +
                        "optaplanner.benchmark.solver.termination.best-score-limit, " +
                        "optaplanner.benchmark.solver.termination.unimproved-spent-limit " +
                        "is required if termination is not configured in the " +
                        "inherited solver benchmark config and solverBenchmarkBluePrint is used.");
            }
            for (int i = 0; i < solverBenchmarkConfigList.size(); i++) {
                SolverBenchmarkConfig solverBenchmarkConfig = solverBenchmarkConfigList.get(i);
                TerminationConfig terminationConfig = solverBenchmarkConfig.getSolverConfig().getTerminationConfig();
                if (terminationConfig == null || !terminationConfig.isConfigured()) {
                    boolean isTerminationConfiguredForAllNonConstructionHeuristicPhases = !solverBenchmarkConfig
                            .getSolverConfig().getPhaseConfigList().isEmpty();
                    for (PhaseConfig<?> phaseConfig : solverBenchmarkConfig.getSolverConfig().getPhaseConfigList()) {
                        if (!(phaseConfig instanceof ConstructionHeuristicPhaseConfig)) {
                            if (phaseConfig.getTerminationConfig() == null
                                    || !phaseConfig.getTerminationConfig().isConfigured()) {
                                isTerminationConfiguredForAllNonConstructionHeuristicPhases = false;
                                break;
                            }
                        }
                    }
                    if (!isTerminationConfiguredForAllNonConstructionHeuristicPhases) {
                        String benchmarkConfigName = solverBenchmarkConfig.getName();
                        if (benchmarkConfigName == null) {
                            benchmarkConfigName = "SolverBenchmarkConfig " + i;
                        }
                        unconfiguredTerminationSolverBenchmarkList.add(benchmarkConfigName);
                    }
                }
            }
            if (!unconfiguredTerminationSolverBenchmarkList.isEmpty()) {
                throw new IllegalStateException("The following " + SolverBenchmarkConfig.class.getSimpleName() + " do not " +
                        "have termination configured: " +
                        unconfiguredTerminationSolverBenchmarkList.stream()
                                .collect(Collectors.joining(", ", "[", "]"))
                        + ". " +
                        "At least one of the properties " +
                        "optaplanner.benchmark.solver.termination.spent-limit, " +
                        "optaplanner.benchmark.solver.termination.best-score-limit, " +
                        "optaplanner.benchmark.solver.termination.unimproved-spent-limit " +
                        "is required if termination is not configured in a solver benchmark and the " +
                        "inherited solver benchmark config.");
            }
        }
        return benchmarkConfig;
    }

    @Bean
    public PlannerBenchmarkFactory plannerBenchmarkFactory(PlannerBenchmarkConfig benchmarkConfig) {
        return PlannerBenchmarkFactory.create(benchmarkConfig);
    }

    @Override
    public void setBeanClassLoader(ClassLoader classLoader) {
        this.beanClassLoader = classLoader;
    }
}
