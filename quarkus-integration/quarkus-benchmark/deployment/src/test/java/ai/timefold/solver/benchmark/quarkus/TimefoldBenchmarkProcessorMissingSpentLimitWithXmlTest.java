package ai.timefold.solver.benchmark.quarkus;

import static org.assertj.core.api.Assertions.assertThatCode;

import ai.timefold.solver.benchmark.config.PlannerBenchmarkConfig;
import ai.timefold.solver.benchmark.quarkus.testdata.normal.constraints.TestdataQuarkusConstraintProvider;
import ai.timefold.solver.benchmark.quarkus.testdata.normal.domain.TestdataQuarkusEntity;
import ai.timefold.solver.benchmark.quarkus.testdata.normal.domain.TestdataQuarkusSolution;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

class TimefoldBenchmarkProcessorMissingSpentLimitWithXmlTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .overrideConfigKey("quarkus.timefold.benchmark.solver-benchmark-config-xml",
                    "emptySolverBenchmarkConfig.xml")
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addAsResource("emptySolverBenchmarkConfig.xml")
                    .addClasses(TestdataQuarkusEntity.class,
                            TestdataQuarkusSolution.class, TestdataQuarkusConstraintProvider.class));

    @Test
    void benchmark() throws InterruptedException {
        assertThatCode(() -> new TimefoldBenchmarkRecorder().benchmarkConfigSupplier(new PlannerBenchmarkConfig(), null).get())
                .hasMessageContainingAll("At least one of the properties",
                        "quarkus.timefold.benchmark.solver.termination.spent-limit",
                        "quarkus.timefold.benchmark.solver.termination.best-score-limit",
                        "quarkus.timefold.benchmark.solver.termination.unimproved-spent-limit",
                        "is required if termination is not configured");
    }
}
