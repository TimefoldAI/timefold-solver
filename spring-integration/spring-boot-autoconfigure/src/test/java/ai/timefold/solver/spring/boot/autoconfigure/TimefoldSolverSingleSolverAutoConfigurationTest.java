package ai.timefold.solver.spring.boot.autoconfigure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.stream.IntStream;

import ai.timefold.solver.benchmark.api.PlannerBenchmarkFactory;
import ai.timefold.solver.core.api.score.buildin.hardsoft.HardSoftScore;
import ai.timefold.solver.core.api.solver.SolverConfigOverride;
import ai.timefold.solver.core.api.solver.SolverFactory;
import ai.timefold.solver.core.api.solver.SolverManager;
import ai.timefold.solver.core.config.solver.SolverConfig;
import ai.timefold.solver.core.config.solver.termination.TerminationConfig;
import ai.timefold.solver.core.impl.solver.DefaultSolverJob;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;
import ai.timefold.solver.spring.boot.autoconfigure.chained.ChainedSpringTestConfiguration;
import ai.timefold.solver.spring.boot.autoconfigure.chained.constraints.TestdataChainedSpringConstraintProvider;
import ai.timefold.solver.spring.boot.autoconfigure.chained.domain.TestdataChainedSpringAnchor;
import ai.timefold.solver.spring.boot.autoconfigure.chained.domain.TestdataChainedSpringEntity;
import ai.timefold.solver.spring.boot.autoconfigure.chained.domain.TestdataChainedSpringObject;
import ai.timefold.solver.spring.boot.autoconfigure.chained.domain.TestdataChainedSpringSolution;
import ai.timefold.solver.spring.boot.autoconfigure.config.TimefoldProperties;
import ai.timefold.solver.spring.boot.autoconfigure.declarative.SupplierVariableSpringTestConfiguration;
import ai.timefold.solver.spring.boot.autoconfigure.declarative.domain.TestdataSpringSupplierVariableEntity;
import ai.timefold.solver.spring.boot.autoconfigure.declarative.domain.TestdataSpringSupplierVariableSolution;
import ai.timefold.solver.spring.boot.autoconfigure.missingsuppliervariable.MissingSupplierVariableSpringTestConfiguration;
import ai.timefold.solver.spring.boot.autoconfigure.multimodule.MultiModuleSpringTestConfiguration;
import ai.timefold.solver.spring.boot.autoconfigure.normal.NormalSpringTestConfiguration;
import ai.timefold.solver.spring.boot.autoconfigure.normal.constraints.TestdataSpringConstraintProvider;
import ai.timefold.solver.spring.boot.autoconfigure.normal.domain.TestdataSpringEntity;
import ai.timefold.solver.spring.boot.autoconfigure.normal.domain.TestdataSpringSolution;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.TestExecutionListeners;

@TestExecutionListeners
@Execution(ExecutionMode.CONCURRENT)
class TimefoldSolverSingleSolverAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner;
    private final ApplicationContextRunner chainedContextRunner;
    private final ApplicationContextRunner supplierVariableContextRunner;
    private final ApplicationContextRunner missingSupplierVariableContextRunner;
    private final ApplicationContextRunner multimoduleRunner;
    private final ApplicationContextRunner benchmarkContextRunner;
    private final FilteredClassLoader allDefaultsFilteredClassLoader;

    public TimefoldSolverSingleSolverAutoConfigurationTest() {
        contextRunner = new ApplicationContextRunner()
                .withConfiguration(
                        AutoConfigurations.of(TimefoldSolverAutoConfiguration.class, TimefoldSolverBeanFactory.class))
                .withUserConfiguration(NormalSpringTestConfiguration.class);
        chainedContextRunner = new ApplicationContextRunner()
                .withConfiguration(
                        AutoConfigurations.of(TimefoldSolverAutoConfiguration.class, TimefoldSolverBeanFactory.class))
                .withUserConfiguration(ChainedSpringTestConfiguration.class);
        supplierVariableContextRunner = new ApplicationContextRunner()
                .withConfiguration(
                        AutoConfigurations.of(TimefoldSolverAutoConfiguration.class, TimefoldSolverBeanFactory.class))
                .withUserConfiguration(SupplierVariableSpringTestConfiguration.class);
        missingSupplierVariableContextRunner = new ApplicationContextRunner()
                .withConfiguration(
                        AutoConfigurations.of(TimefoldSolverAutoConfiguration.class, TimefoldSolverBeanFactory.class))
                .withUserConfiguration(MissingSupplierVariableSpringTestConfiguration.class);
        multimoduleRunner = new ApplicationContextRunner()
                .withConfiguration(
                        AutoConfigurations.of(TimefoldSolverAutoConfiguration.class, TimefoldSolverBeanFactory.class))
                .withUserConfiguration(MultiModuleSpringTestConfiguration.class);
        benchmarkContextRunner = new ApplicationContextRunner()
                .withConfiguration(
                        AutoConfigurations.of(TimefoldSolverAutoConfiguration.class, TimefoldSolverBeanFactory.class,
                                TimefoldBenchmarkAutoConfiguration.class))
                .withUserConfiguration(NormalSpringTestConfiguration.class);
        allDefaultsFilteredClassLoader =
                new FilteredClassLoader(FilteredClassLoader.PackageFilter.of("ai.timefold.solver.test"),
                        FilteredClassLoader.ClassPathResourceFilter
                                .of(new ClassPathResource(TimefoldProperties.DEFAULT_SOLVER_CONFIG_URL)));
    }

    @Test
    void solverConfigXml_none() {
        contextRunner
                .withClassLoader(allDefaultsFilteredClassLoader)
                .run(context -> {
                    var solverConfig = context.getBean(SolverConfig.class);
                    assertThat(solverConfig).isNotNull();
                    assertThat(solverConfig.getSolutionClass()).isEqualTo(TestdataSpringSolution.class);
                    assertThat(solverConfig.getEntityClassList())
                            .isEqualTo(Collections.singletonList(TestdataSpringEntity.class));
                    assertThat(solverConfig.getScoreDirectorFactoryConfig().getConstraintProviderClass())
                            .isEqualTo(TestdataSpringConstraintProvider.class);
                    // No termination defined
                    assertThat(solverConfig.getTerminationConfig()).isNull();
                    var solverFactory = context.getBean(SolverFactory.class);
                    assertThat(solverFactory).isNotNull();
                    assertThat(solverFactory.buildSolver()).isNotNull();
                });
    }

    @Test
    void solve() {
        contextRunner
                .withClassLoader(allDefaultsFilteredClassLoader)
                .withPropertyValues("timefold.solver.termination.best-score-limit=0")
                .run(context -> {
                    SolverManager<TestdataSpringSolution, Long> solverManager = context.getBean(SolverManager.class);
                    var problem = new TestdataSpringSolution();
                    problem.setValueList(IntStream.range(1, 3)
                            .mapToObj(i -> "v" + i)
                            .toList());
                    problem.setEntityList(IntStream.range(1, 3)
                            .mapToObj(i -> new TestdataSpringEntity())
                            .toList());
                    var solverJob = solverManager.solve(1L, problem);
                    var solution = solverJob.getFinalBestSolution();
                    assertThat(solution).isNotNull();
                    assertThat(solution.getScore().score()).isNotNegative();
                });
    }

    @Test
    void solveWithParallelSolverCount() {
        contextRunner
                .withClassLoader(allDefaultsFilteredClassLoader)
                .withPropertyValues("timefold.solver-manager.parallel-solver-count=2")
                .run(context -> {
                    var solverManager = context.getBean(SolverManager.class);
                    assertThat(solverManager).isNotNull();
                });
    }

    @Test
    void solveWithTimeOverride() {
        contextRunner
                .withClassLoader(allDefaultsFilteredClassLoader)
                .withPropertyValues("timefold.solver.termination.best-score-limit=0",
                        "timefold.solver.termination.spent-limit=30s")
                .run(context -> {
                    var solverManager = context.getBean(SolverManager.class);
                    var problem = new TestdataSpringSolution();
                    problem.setValueList(IntStream.range(1, 3)
                            .mapToObj(i -> "v" + i)
                            .toList());
                    problem.setEntityList(IntStream.range(1, 3)
                            .mapToObj(i -> new TestdataSpringEntity())
                            .toList());
                    var solverJob =
                            (DefaultSolverJob<TestdataSpringSolution, Long>) solverManager.solveBuilder()
                                    .withProblemId(1L)
                                    .withProblem(problem)
                                    .withConfigOverride(
                                            new SolverConfigOverride<TestdataSpringSolution>()
                                                    .withTerminationConfig(new TerminationConfig()
                                                            .withSpentLimit(Duration.ofSeconds(2L))))
                                    .run();
                    SolverScope<TestdataSpringSolution> customScope = new SolverScope<>() {
                        @Override
                        public long calculateTimeMillisSpentUpToNow() {
                            // Return one second to make the time gradient predictable
                            return 1000L;
                        }
                    };
                    // We ensure the best-score limit won't take priority
                    customScope.setStartingInitializedScore(HardSoftScore.of(-1, -1));
                    customScope.setInitializedBestScore(HardSoftScore.of(-1, -1));
                    var gradientTime = solverJob.getSolverTermination().calculateSolverTimeGradient(customScope);
                    var solution = solverJob.getFinalBestSolution();
                    assertThat(solution).isNotNull();
                    assertThat(solution.getScore().score()).isNotNegative();
                    // Spent-time is 30s by default, but it is overridden with 2. The gradient time must be 50%
                    assertThat(gradientTime).isEqualTo(0.5);
                });
    }

    @Test
    void multimoduleSolve() {
        multimoduleRunner
                .withClassLoader(allDefaultsFilteredClassLoader)
                .withPropertyValues("timefold.solver.termination.best-score-limit=0")
                .run(context -> {
                    SolverManager<TestdataSpringSolution, Long> solverManager = context.getBean(SolverManager.class);
                    var problem = new TestdataSpringSolution();
                    problem.setValueList(IntStream.range(1, 3)
                            .mapToObj(i -> "v" + i)
                            .toList());
                    problem.setEntityList(IntStream.range(1, 3)
                            .mapToObj(i -> new TestdataSpringEntity())
                            .toList());
                    var solverJob = solverManager.solve(1L, problem);
                    var solution = solverJob.getFinalBestSolution();
                    assertThat(solution).isNotNull();
                    assertThat(solution.getScore().score()).isNotNegative();
                });
    }

    @Test
    void chained_solverConfigXml_none() {
        chainedContextRunner
                .withClassLoader(allDefaultsFilteredClassLoader)
                .run(context -> {
                    var solverConfig = context.getBean(SolverConfig.class);
                    assertThat(solverConfig).isNotNull();
                    assertThat(solverConfig.getSolutionClass()).isEqualTo(TestdataChainedSpringSolution.class);
                    assertThat(solverConfig.getEntityClassList()).containsExactlyInAnyOrder(
                            TestdataChainedSpringObject.class,
                            TestdataChainedSpringEntity.class,
                            TestdataChainedSpringAnchor.class);
                    assertThat(solverConfig.getScoreDirectorFactoryConfig().getConstraintProviderClass())
                            .isEqualTo(TestdataChainedSpringConstraintProvider.class);
                    // No termination defined
                    assertThat(solverConfig.getTerminationConfig()).isNull();
                    var solverFactory = context.getBean(SolverFactory.class);
                    assertThat(solverFactory).isNotNull();
                    assertThat(solverFactory.buildSolver()).isNotNull();
                });
    }

    @Test
    void solveSupplierVariables() {
        supplierVariableContextRunner
                .withClassLoader(allDefaultsFilteredClassLoader)
                .withPropertyValues(
                        "timefold.solver.termination.best-score-limit=0")
                .run(context -> {
                    SolverManager<TestdataSpringSupplierVariableSolution, Long> solverManager =
                            context.getBean(SolverManager.class);
                    var problem = new TestdataSpringSupplierVariableSolution();
                    problem.setValueList(List.of("a", "b"));
                    problem.setEntityList(List.of(new TestdataSpringSupplierVariableEntity()));
                    var solverJob = solverManager.solve(1L, problem);
                    var solution = solverJob.getFinalBestSolution();
                    assertThat(solution).isNotNull();
                    assertThat(solution.getScore().score()).isNotNegative();
                });
    }

    @Test
    void missingSupplierVariables() {
        assertThatCode(() -> missingSupplierVariableContextRunner
                .withClassLoader(allDefaultsFilteredClassLoader)
                .withPropertyValues(
                        "timefold.solver.termination.best-score-limit=0")
                .run(context -> context.getBean(SolverFactory.class)))
                .hasMessageContainingAll("@ShadowVariable (value1AndValue2)",
                        "supplierName (value1AndValue2Supplier) that does not exist",
                        "inside its declaring class (ai.timefold.solver.spring.boot.autoconfigure.missingsuppliervariable.domain.TestdataSpringMissingSupplierVariableEntity).",
                        "Maybe you misspelled the supplierName name?");
    }

    @Test
    void benchmarkWithSpentLimit() {
        benchmarkContextRunner
                .withClassLoader(allDefaultsFilteredClassLoader)
                .withPropertyValues("timefold.benchmark.solver.termination.spent-limit=1s")
                .run(context -> {
                    var benchmarkFactory = context.getBean(PlannerBenchmarkFactory.class);
                    var problem = new TestdataSpringSolution();
                    problem.setValueList(IntStream.range(1, 3)
                            .mapToObj(i -> "v" + i)
                            .toList());
                    problem.setEntityList(IntStream.range(1, 3)
                            .mapToObj(i -> new TestdataSpringEntity())
                            .toList());
                    assertThat(benchmarkFactory.buildPlannerBenchmark(problem).benchmark()).isNotEmptyDirectory();
                });
    }

    @Test
    void benchmark() {
        benchmarkContextRunner
                .withClassLoader(allDefaultsFilteredClassLoader)
                .withPropertyValues("timefold.solver.termination.best-score-limit=0")
                .run(context -> {
                    var benchmarkFactory = context.getBean(PlannerBenchmarkFactory.class);
                    var problem = new TestdataSpringSolution();
                    problem.setValueList(IntStream.range(1, 3)
                            .mapToObj(i -> "v" + i)
                            .toList());
                    problem.setEntityList(IntStream.range(1, 3)
                            .mapToObj(i -> new TestdataSpringEntity())
                            .toList());
                    assertThat(benchmarkFactory.buildPlannerBenchmark(problem).benchmark()).isNotEmptyDirectory();
                });
    }

    @Test
    void benchmarkWithXml() {
        benchmarkContextRunner
                .withClassLoader(allDefaultsFilteredClassLoader)
                .withPropertyValues("timefold.benchmark.solver.termination.spent-limit=100ms")
                .withPropertyValues(
                        "timefold.benchmark.solver-benchmark-config-xml=ai/timefold/solver/spring/boot/autoconfigure/solverBenchmarkConfig.xml")
                .run(context -> {
                    var benchmarkFactory = context.getBean(PlannerBenchmarkFactory.class);
                    var problem = new TestdataSpringSolution();
                    problem.setValueList(IntStream.range(1, 3)
                            .mapToObj(i -> "v" + i)
                            .toList());
                    problem.setEntityList(IntStream.range(1, 3)
                            .mapToObj(i -> new TestdataSpringEntity())
                            .toList());
                    assertThat(benchmarkFactory.buildPlannerBenchmark(problem).benchmark()).isNotEmptyDirectory();
                });
    }

    @Test
    void resoucesInjectionFailure() {
        assertThatCode(() -> benchmarkContextRunner
                .withClassLoader(allDefaultsFilteredClassLoader)
                .withPropertyValues("timefold.solver.solver1.termination.best-score-limit=0")
                .withPropertyValues("timefold.solver.solver2.termination.best-score-limit=0")
                .withPropertyValues("timefold.benchmark.solver.termination.spent-limit=1s")
                .run(context -> context.getBean(PlannerBenchmarkFactory.class)))
                .hasRootCauseMessage("""
                        When defining multiple solvers, the benchmark feature is not enabled.
                        Consider using separate <solverBenchmark> instances for evaluating different solver configurations.""");
    }

}
