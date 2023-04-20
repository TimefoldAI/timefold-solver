package ai.timefold.solver.benchmark.quarkus;

import java.util.concurrent.ExecutionException;

import ai.timefold.solver.benchmark.config.PlannerBenchmarkConfig;
import ai.timefold.solver.benchmark.quarkus.testdata.normal.constraints.TestdataQuarkusConstraintProvider;
import ai.timefold.solver.benchmark.quarkus.testdata.normal.domain.TestdataQuarkusEntity;
import ai.timefold.solver.benchmark.quarkus.testdata.normal.domain.TestdataQuarkusSolution;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

class TimefoldBenchmarkProcessorMissingSpentLimitTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .overrideConfigKey("quarkus.test.flat-class-path", "true")
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(TestdataQuarkusEntity.class,
                            TestdataQuarkusSolution.class, TestdataQuarkusConstraintProvider.class));

    @Test
    void benchmark() throws ExecutionException, InterruptedException {
        IllegalStateException exception = Assertions.assertThrows(IllegalStateException.class, () -> {
            new TimefoldBenchmarkRecorder().benchmarkConfigSupplier(new PlannerBenchmarkConfig()).get();
        });
        Assertions.assertEquals(
                "At least one of the properties quarkus.optaplanner.benchmark.solver.termination.spent-limit, quarkus.optaplanner.benchmark.solver.termination.best-score-limit, quarkus.optaplanner.benchmark.solver.termination.unimproved-spent-limit is required if termination is not configured in the inherited solver benchmark config and solverBenchmarkBluePrint is used.",
                exception.getMessage());
    }

}
