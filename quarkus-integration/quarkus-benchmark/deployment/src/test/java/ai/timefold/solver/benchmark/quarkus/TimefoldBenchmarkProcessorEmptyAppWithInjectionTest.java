package ai.timefold.solver.benchmark.quarkus;

import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

import ai.timefold.solver.benchmark.api.PlannerBenchmarkFactory;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.test.QuarkusUnitTest;

class TimefoldBenchmarkProcessorEmptyAppWithInjectionTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .overrideConfigKey("quarkus.timefold.benchmark.solver.termination.best-score-limit", "0")
            .overrideConfigKey("quarkus.arc.unremovable-types", PlannerBenchmarkFactory.class.getName())
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses());

    @Test
    void emptyAppDoesNotCrash() {
        assertThatIllegalStateException().isThrownBy(() -> Arc.container().instance(PlannerBenchmarkFactory.class).get())
                .withMessageContaining("The " + PlannerBenchmarkFactory.class.getName() + " is not available as there are no");
    }

}
