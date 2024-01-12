package ai.timefold.solver.benchmark.quarkus;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

class TimefoldBenchmarkProcessorMultipleSolverEmptyAppTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .overrideConfigKey("quarkus.timefold.benchmark.solver.\"solver1\".termination.best-score-limit", "0")
            .overrideConfigKey("quarkus.timefold.benchmark.solver.\"solver2\".termination.best-score-limit", "0")
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses());

    @Test
    void emptyAppDoesNotCrash() {
        // Success if it didn't crash during bootstrap
    }

}
