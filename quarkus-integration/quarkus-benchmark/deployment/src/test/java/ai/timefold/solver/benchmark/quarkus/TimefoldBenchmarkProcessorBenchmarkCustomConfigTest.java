package ai.timefold.solver.benchmark.quarkus;

import java.util.stream.IntStream;

import jakarta.inject.Inject;

import ai.timefold.solver.benchmark.api.PlannerBenchmarkFactory;
import ai.timefold.solver.benchmark.quarkus.testdata.normal.constraints.TestdataQuarkusConstraintProvider;
import ai.timefold.solver.benchmark.quarkus.testdata.normal.domain.TestdataQuarkusEntity;
import ai.timefold.solver.benchmark.quarkus.testdata.normal.domain.TestdataQuarkusSolution;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

class TimefoldBenchmarkProcessorBenchmarkCustomConfigTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .overrideConfigKey("quarkus.timefold.benchmark.solver.termination.best-score-limit", "0")
            .overrideConfigKey("quarkus.timefold.benchmark.solver-benchmark-config-xml", "customSolverBenchmarkConfig.xml")
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addAsResource("customSolverBenchmarkConfig.xml")
                    .addClasses(TestdataQuarkusEntity.class,
                            TestdataQuarkusSolution.class, TestdataQuarkusConstraintProvider.class));

    @Inject
    PlannerBenchmarkFactory benchmarkFactory;

    @Test
    @SuppressWarnings("java:S2699")
    void benchmark() {
        TestdataQuarkusSolution problem = new TestdataQuarkusSolution();
        problem.setValueList(IntStream.range(1, 3)
                .mapToObj(i -> "v" + i)
                .toList());
        problem.setEntityList(IntStream.range(1, 3)
                .mapToObj(i -> new TestdataQuarkusEntity())
                .toList());
        benchmarkFactory.buildPlannerBenchmark(problem).benchmark();
    }

}
