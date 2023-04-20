package ai.timefold.solver.benchmark.quarkus;

import java.util.concurrent.ExecutionException;

import ai.timefold.solver.benchmark.config.PlannerBenchmarkConfig;
import ai.timefold.solver.benchmark.config.SolverBenchmarkConfig;
import ai.timefold.solver.benchmark.quarkus.testdata.normal.constraints.TestdataQuarkusConstraintProvider;
import ai.timefold.solver.benchmark.quarkus.testdata.normal.domain.TestdataQuarkusEntity;
import ai.timefold.solver.benchmark.quarkus.testdata.normal.domain.TestdataQuarkusSolution;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

class TimefoldBenchmarkProcessorMissingSpentLimitPerBenchmarkTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .overrideConfigKey("quarkus.test.flat-class-path", "true")
            .overrideConfigKey("quarkus.timefold.benchmark.solver-benchmark-config-xml",
                    "solverBenchmarkConfigSpentLimitPerBenchmarkNoTermination.xml")
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(TestdataQuarkusEntity.class,
                            TestdataQuarkusSolution.class, TestdataQuarkusConstraintProvider.class)
                    .addAsResource("solverBenchmarkConfigSpentLimitPerBenchmarkNoTermination.xml"));

    @Test
    void benchmark() throws ExecutionException, InterruptedException {
        PlannerBenchmarkConfig benchmarkConfig =
                PlannerBenchmarkConfig.createFromXmlResource("solverBenchmarkConfigSpentLimitPerBenchmarkNoTermination.xml");
        IllegalStateException exception = Assertions.assertThrows(IllegalStateException.class, () -> {
            new TimefoldBenchmarkRecorder().benchmarkConfigSupplier(benchmarkConfig).get();
        });
        Assertions.assertEquals(
                "The following " + SolverBenchmarkConfig.class.getSimpleName() + " do not " +
                        "have termination configured: [First Fit and Local Search without Termination]. " +
                        "At least one of the properties " +
                        "quarkus.timefold.benchmark.solver.termination.spent-limit, " +
                        "quarkus.timefold.benchmark.solver.termination.best-score-limit, " +
                        "quarkus.timefold.benchmark.solver.termination.unimproved-spent-limit " +
                        "is required if termination is not configured in a solver benchmark and the " +
                        "inherited solver benchmark config.",
                exception.getMessage());
    }

}
