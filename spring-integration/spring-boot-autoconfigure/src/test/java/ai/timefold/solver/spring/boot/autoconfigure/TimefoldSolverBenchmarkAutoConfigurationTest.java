package ai.timefold.solver.spring.boot.autoconfigure;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.stream.IntStream;

import ai.timefold.solver.benchmark.api.PlannerBenchmarkFactory;
import ai.timefold.solver.spring.boot.autoconfigure.config.TimefoldProperties;
import ai.timefold.solver.spring.boot.autoconfigure.normal.NormalSpringTestConfiguration;
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
class TimefoldSolverBenchmarkAutoConfigurationTest {

    private final ApplicationContextRunner benchmarkContextRunner;
    private final FilteredClassLoader allDefaultsFilteredClassLoader;

    public TimefoldSolverBenchmarkAutoConfigurationTest() {
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

}
