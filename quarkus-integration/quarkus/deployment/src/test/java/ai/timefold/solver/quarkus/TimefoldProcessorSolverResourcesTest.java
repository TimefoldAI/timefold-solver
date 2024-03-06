package ai.timefold.solver.quarkus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;

import jakarta.inject.Inject;

import ai.timefold.solver.core.api.domain.common.DomainAccessType;
import ai.timefold.solver.core.api.score.ScoreManager;
import ai.timefold.solver.core.api.score.buildin.bendable.BendableScore;
import ai.timefold.solver.core.api.score.buildin.bendablebigdecimal.BendableBigDecimalScore;
import ai.timefold.solver.core.api.score.buildin.bendablelong.BendableLongScore;
import ai.timefold.solver.core.api.score.buildin.hardmediumsoft.HardMediumSoftScore;
import ai.timefold.solver.core.api.score.buildin.hardmediumsoftbigdecimal.HardMediumSoftBigDecimalScore;
import ai.timefold.solver.core.api.score.buildin.hardmediumsoftlong.HardMediumSoftLongScore;
import ai.timefold.solver.core.api.score.buildin.hardsoft.HardSoftScore;
import ai.timefold.solver.core.api.score.buildin.hardsoftbigdecimal.HardSoftBigDecimalScore;
import ai.timefold.solver.core.api.score.buildin.hardsoftlong.HardSoftLongScore;
import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.api.score.buildin.simplebigdecimal.SimpleBigDecimalScore;
import ai.timefold.solver.core.api.score.buildin.simplelong.SimpleLongScore;
import ai.timefold.solver.core.api.solver.SolutionManager;
import ai.timefold.solver.core.api.solver.SolverFactory;
import ai.timefold.solver.core.api.solver.SolverManager;
import ai.timefold.solver.core.config.solver.EnvironmentMode;
import ai.timefold.solver.core.config.solver.SolverConfig;
import ai.timefold.solver.quarkus.testdata.normal.constraints.TestdataQuarkusConstraintProvider;
import ai.timefold.solver.quarkus.testdata.normal.domain.TestdataQuarkusEntity;
import ai.timefold.solver.quarkus.testdata.normal.domain.TestdataQuarkusSolution;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

class TimefoldProcessorSolverResourcesTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .overrideConfigKey("quarkus.timefold.solver.\"solver1\".environment-mode", "FULL_ASSERT")
            .overrideConfigKey("quarkus.timefold.solver.\"solver1\".daemon", "true")
            .overrideConfigKey("quarkus.timefold.solver.\"solver1\".domain-access-type", "REFLECTION")
            .overrideConfigKey("quarkus.timefold.solver.\"solver1\".termination.spent-limit", "4h")
            .overrideConfigKey("quarkus.timefold.solver.\"solver1\".termination.unimproved-spent-limit", "5h")
            .overrideConfigKey("quarkus.timefold.solver.\"solver1\".termination.best-score-limit", "0")
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(TestdataQuarkusEntity.class, TestdataQuarkusSolution.class,
                            TestdataQuarkusConstraintProvider.class));

    @Inject
    SolverConfig solverConfig;
    @Inject
    SolverFactory<TestdataQuarkusSolution> solver1Factory;
    @Inject
    SolverManager<TestdataQuarkusSolution, String> solver1Manager;

    // SolutionManager per score type
    @Inject
    SolutionManager<TestdataQuarkusSolution, SimpleScore> simpleSolutionManager1;
    @Inject
    SolutionManager<TestdataQuarkusSolution, SimpleLongScore> simpleLongSolutionManager1;
    @Inject
    SolutionManager<TestdataQuarkusSolution, SimpleBigDecimalScore> simpleBigDecimalSolutionManager1;
    @Inject
    SolutionManager<TestdataQuarkusSolution, HardSoftScore> hardSoftSolutionManager1;
    @Inject
    SolutionManager<TestdataQuarkusSolution, HardSoftLongScore> hardSoftLongSolutionManager1;
    @Inject
    SolutionManager<TestdataQuarkusSolution, HardSoftBigDecimalScore> hardSoftBigDecimalSolutionManager1;
    @Inject
    SolutionManager<TestdataQuarkusSolution, HardMediumSoftScore> hardMediumSoftSolutionManager1;
    @Inject
    SolutionManager<TestdataQuarkusSolution, HardMediumSoftLongScore> hardMediumSoftLongSolutionManager1;
    @Inject
    SolutionManager<TestdataQuarkusSolution, HardMediumSoftBigDecimalScore> hardMediumSoftBigDecimalSolutionManager1;
    @Inject
    SolutionManager<TestdataQuarkusSolution, BendableScore> bendableSolutionManager1;
    @Inject
    SolutionManager<TestdataQuarkusSolution, BendableLongScore> bendableLongSolutionManager1;
    @Inject
    SolutionManager<TestdataQuarkusSolution, BendableBigDecimalScore> bendableBigDecimalSolutionManager1;

    // ScoreManager per score type
    @Inject
    ScoreManager<TestdataQuarkusSolution, SimpleScore> simpleScoreManager1;
    @Inject
    ScoreManager<TestdataQuarkusSolution, SimpleLongScore> simpleLongScoreManager1;
    @Inject
    ScoreManager<TestdataQuarkusSolution, SimpleBigDecimalScore> simpleBigDecimalScoreManager1;
    @Inject
    ScoreManager<TestdataQuarkusSolution, HardSoftScore> hardSoftScoreManager1;
    @Inject
    ScoreManager<TestdataQuarkusSolution, HardSoftLongScore> hardSoftLongScoreManager1;
    @Inject
    ScoreManager<TestdataQuarkusSolution, HardSoftBigDecimalScore> hardSoftBigDecimalScoreManager1;
    @Inject
    ScoreManager<TestdataQuarkusSolution, HardMediumSoftScore> hardMediumSoftScoreManager1;
    @Inject
    ScoreManager<TestdataQuarkusSolution, HardMediumSoftLongScore> hardMediumSoftLongScoreManager1;
    @Inject
    ScoreManager<TestdataQuarkusSolution, HardMediumSoftBigDecimalScore> hardMediumSoftBigDecimalScoreManager1;
    @Inject
    ScoreManager<TestdataQuarkusSolution, BendableScore> bendableScoreManager1;
    @Inject
    ScoreManager<TestdataQuarkusSolution, BendableLongScore> bendableLongScoreManager1;
    @Inject
    ScoreManager<TestdataQuarkusSolution, BendableBigDecimalScore> bendableBigDecimalScoreManager1;

    @Test
    void solverProperties() {
        // solver1
        assertEquals(EnvironmentMode.FULL_ASSERT, solverConfig.getEnvironmentMode());
        assertNull(solverConfig.getNearbyDistanceMeterClass());
        assertTrue(solverConfig.getDaemon());
        assertEquals(DomainAccessType.REFLECTION, solverConfig.getDomainAccessType());
        assertEquals(null,
                solverConfig.getScoreDirectorFactoryConfig().getConstraintStreamImplType());
        assertNotNull(solver1Factory);
        assertEquals(Duration.ofHours(4), solverConfig.getTerminationConfig().getSpentLimit());
        assertEquals(Duration.ofHours(5), solverConfig.getTerminationConfig().getUnimprovedSpentLimit());
        assertEquals(SimpleScore.of(0).toString(), solverConfig.getTerminationConfig().getBestScoreLimit());
        assertNotNull(solver1Factory);
        assertNotNull(solver1Manager);
        // SolutionManager
        assertNotNull(simpleSolutionManager1);
        assertNotNull(simpleLongSolutionManager1);
        assertNotNull(simpleBigDecimalSolutionManager1);
        assertNotNull(hardSoftSolutionManager1);
        assertNotNull(hardSoftLongSolutionManager1);
        assertNotNull(hardSoftBigDecimalSolutionManager1);
        assertNotNull(hardMediumSoftSolutionManager1);
        assertNotNull(hardMediumSoftLongSolutionManager1);
        assertNotNull(hardMediumSoftBigDecimalSolutionManager1);
        assertNotNull(bendableSolutionManager1);
        assertNotNull(bendableLongSolutionManager1);
        assertNotNull(bendableBigDecimalSolutionManager1);
        // ScoreManager
        assertNotNull(simpleScoreManager1);
        assertNotNull(simpleLongScoreManager1);
        assertNotNull(simpleBigDecimalScoreManager1);
        assertNotNull(hardSoftScoreManager1);
        assertNotNull(hardSoftLongScoreManager1);
        assertNotNull(hardSoftBigDecimalScoreManager1);
        assertNotNull(hardMediumSoftScoreManager1);
        assertNotNull(hardMediumSoftLongScoreManager1);
        assertNotNull(hardMediumSoftBigDecimalScoreManager1);
        assertNotNull(bendableScoreManager1);
        assertNotNull(bendableLongScoreManager1);
        assertNotNull(bendableBigDecimalScoreManager1);
    }
}
