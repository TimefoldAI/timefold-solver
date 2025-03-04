package ai.timefold.solver.quarkus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;

import jakarta.inject.Inject;

import ai.timefold.solver.core.api.domain.common.DomainAccessType;
import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.api.solver.SolverFactory;
import ai.timefold.solver.core.config.solver.EnvironmentMode;
import ai.timefold.solver.core.config.solver.SolverConfig;
import ai.timefold.solver.quarkus.testdata.dummy.DummyDistanceMeter;
import ai.timefold.solver.quarkus.testdata.normal.constraints.TestdataQuarkusConstraintProvider;
import ai.timefold.solver.quarkus.testdata.normal.domain.TestdataQuarkusEntity;
import ai.timefold.solver.quarkus.testdata.normal.domain.TestdataQuarkusSolution;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

class TimefoldProcessorSolverPropertiesTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .overrideConfigKey("quarkus.timefold.solver.environment-mode", "FULL_ASSERT")
            .overrideConfigKey("quarkus.timefold.solver.daemon", "true")
            .overrideConfigKey("quarkus.timefold.solver.nearby-distance-meter-class",
                    "ai.timefold.solver.quarkus.testdata.dummy.DummyDistanceMeter")
            .overrideConfigKey("quarkus.timefold.solver.move-thread-count", "2")
            .overrideConfigKey("quarkus.timefold.solver.domain-access-type", "REFLECTION")
            .overrideConfigKey("quarkus.timefold.solver.termination.spent-limit", "4h")
            .overrideConfigKey("quarkus.timefold.solver.termination.unimproved-spent-limit", "5h")
            .overrideConfigKey("quarkus.timefold.solver.termination.best-score-limit", "0")
            .overrideConfigKey("quarkus.timefold.solver.termination.diminished-returns.enabled", "true")
            .overrideConfigKey("quarkus.timefold.solver.termination.diminished-returns.sliding-window-duration", "6h")
            .overrideConfigKey("quarkus.timefold.solver.termination.diminished-returns.minimum-improvement-ratio", "0.5")
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(TestdataQuarkusEntity.class, TestdataQuarkusSolution.class,
                            TestdataQuarkusConstraintProvider.class, DummyDistanceMeter.class));

    @Inject
    SolverConfig solverConfig;
    @Inject
    SolverFactory<TestdataQuarkusSolution> solverFactory;

    @Test
    void solverProperties() {
        assertEquals(EnvironmentMode.FULL_ASSERT, solverConfig.getEnvironmentMode());
        assertTrue(solverConfig.getDaemon());
        assertEquals("2", solverConfig.getMoveThreadCount());
        assertEquals(DomainAccessType.REFLECTION, solverConfig.getDomainAccessType());
        assertEquals(null,
                solverConfig.getScoreDirectorFactoryConfig().getConstraintStreamImplType());
        assertNotNull(solverConfig.getNearbyDistanceMeterClass());
        assertNotNull(solverFactory);
    }

    @Test
    void terminationProperties() {
        assertEquals(Duration.ofHours(4), solverConfig.getTerminationConfig().getSpentLimit());
        assertEquals(Duration.ofHours(5), solverConfig.getTerminationConfig().getUnimprovedSpentLimit());
        assertEquals(SimpleScore.of(0).toString(), solverConfig.getTerminationConfig().getBestScoreLimit());

        var terminationConfig = solverConfig.getTerminationConfig();
        assertNotNull(terminationConfig);
        assertNotNull(terminationConfig.getDiminishedReturnsConfig());
        assertEquals(Duration.ofHours(6), terminationConfig.getDiminishedReturnsConfig().getSlidingWindowDuration());
        assertEquals(0.5, terminationConfig.getDiminishedReturnsConfig().getMinimumImprovementRatio());
    }
}
