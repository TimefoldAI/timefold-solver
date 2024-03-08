package ai.timefold.solver.quarkus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

import ai.timefold.solver.quarkus.testdata.dummy.DummyDistanceMeter;
import ai.timefold.solver.quarkus.testdata.normal.constraints.TestdataQuarkusConstraintProvider;
import ai.timefold.solver.quarkus.testdata.normal.domain.TestdataQuarkusEntity;
import ai.timefold.solver.quarkus.testdata.normal.domain.TestdataQuarkusSolution;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.smallrye.config.ConfigValidationException;

class TimefoldProcessorSolverInvalidNearbyClassTest {

    // Class not found
    @RegisterExtension
    static final QuarkusUnitTest config1 = new QuarkusUnitTest()
            .overrideConfigKey("quarkus.timefold.solver.environment-mode", "FULL_ASSERT")
            .overrideConfigKey("quarkus.timefold.solver.daemon", "true")
            .overrideConfigKey("quarkus.timefold.solver.nearby-distance-meter-class",
                    "ai.timefold.solver.quarkus.testdata.dummy.DummyDistanceMeter")
            .overrideConfigKey("quarkus.timefold.solver.move-thread-count", "2")
            .overrideConfigKey("quarkus.timefold.solver.domain-access-type", "REFLECTION")
            .overrideConfigKey("quarkus.timefold.solver.termination.spent-limit", "4h")
            .overrideConfigKey("quarkus.timefold.solver.termination.unimproved-spent-limit", "5h")
            .overrideConfigKey("quarkus.timefold.solver.termination.best-score-limit", "0")
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(TestdataQuarkusEntity.class, TestdataQuarkusSolution.class,
                            TestdataQuarkusConstraintProvider.class))
            .assertException(t -> assertThat(t)
                    .isInstanceOf(ConfigValidationException.class)
                    .hasMessageContaining(
                            "The config property quarkus.timefold.solver.nearby-distance-meter-class with the config value")
                    .hasMessageContaining(DummyDistanceMeter.class.getName())
                    .hasMessageContaining("not found"));

    // Invalid Nearby Meter class
    @RegisterExtension
    static final QuarkusUnitTest config2 = new QuarkusUnitTest()
            .overrideConfigKey("quarkus.timefold.solver.environment-mode", "FULL_ASSERT")
            .overrideConfigKey("quarkus.timefold.solver.daemon", "true")
            .overrideConfigKey("quarkus.timefold.solver.nearby-distance-meter-class",
                    "ai.timefold.solver.quarkus.testdata.normal.domain.TestdataQuarkusSolution")
            .overrideConfigKey("quarkus.timefold.solver.move-thread-count", "2")
            .overrideConfigKey("quarkus.timefold.solver.domain-access-type", "REFLECTION")
            .overrideConfigKey("quarkus.timefold.solver.termination.spent-limit", "4h")
            .overrideConfigKey("quarkus.timefold.solver.termination.unimproved-spent-limit", "5h")
            .overrideConfigKey("quarkus.timefold.solver.termination.best-score-limit", "0")
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(TestdataQuarkusEntity.class, TestdataQuarkusSolution.class,
                            TestdataQuarkusConstraintProvider.class))
            .assertException(t -> assertThat(t)
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining(
                            "The Nearby Selection Meter class")
                    .hasMessageContaining(TestdataQuarkusSolution.class.getName())
                    .hasMessageContaining("of the solver config (default) does not implement NearbyDistanceMeter"));

    @Test
    void test() {
        fail("Should not call this method.");
    }
}
