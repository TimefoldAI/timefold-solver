package ai.timefold.solver.quarkus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;

import jakarta.inject.Inject;
import jakarta.inject.Named;

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

class TimefoldProcessorMultipleSolverPropertiesTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .overrideConfigKey("quarkus.timefold.solver.\"solver1\".environment-mode", "FULL_ASSERT")
            .overrideConfigKey("quarkus.timefold.solver.\"solver1\".daemon", "true")
            .overrideConfigKey("quarkus.timefold.solver.\"solver1\".domain-access-type", "REFLECTION")
            .overrideConfigKey("quarkus.timefold.solver.\"solver1\".termination.spent-limit", "4h")
            .overrideConfigKey("quarkus.timefold.solver.\"solver1\".termination.unimproved-spent-limit", "5h")
            .overrideConfigKey("quarkus.timefold.solver.\"solver1\".termination.best-score-limit", "0")
            .overrideConfigKey("quarkus.timefold.solver.\"solver2\".daemon", "false")
            .overrideConfigKey("quarkus.timefold.solver.\"solver2\".domain-access-type", "REFLECTION")
            .overrideConfigKey("quarkus.timefold.solver.\"solver2\".termination.spent-limit", "1h")
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(TestdataQuarkusEntity.class, TestdataQuarkusSolution.class,
                            TestdataQuarkusConstraintProvider.class));

    @Inject
    @Named("solver1Config")
    SolverConfig solverConfig;
    @Inject
    @Named("solver1Factory")
    SolverFactory<TestdataQuarkusSolution> solver1Factory;
    @Inject
    @Named("solver1Manager")
    SolverManager<TestdataQuarkusSolution, String> solver1Manager;

    // ScoreManager per score type
    @Inject
    @Named("solver1ScoreManager")
    ScoreManager<TestdataQuarkusSolution, SimpleScore> simpleScoreManager1;
    @Inject
    @Named("solver1ScoreManager")
    ScoreManager<TestdataQuarkusSolution, SimpleLongScore> simpleLongScoreManager1;
    @Inject
    @Named("solver1ScoreManager")
    ScoreManager<TestdataQuarkusSolution, SimpleBigDecimalScore> simpleBigDecimalScoreManager1;
    @Inject
    @Named("solver1ScoreManager")
    ScoreManager<TestdataQuarkusSolution, HardSoftScore> hardSoftScoreManager1;
    @Inject
    @Named("solver1ScoreManager")
    ScoreManager<TestdataQuarkusSolution, HardSoftLongScore> hardSoftLongScoreManager1;
    @Inject
    @Named("solver1ScoreManager")
    ScoreManager<TestdataQuarkusSolution, HardSoftBigDecimalScore> hardSoftBigDecimalScoreManager1;
    @Inject
    @Named("solver1ScoreManager")
    ScoreManager<TestdataQuarkusSolution, HardMediumSoftScore> hardMediumSoftScoreManager1;
    @Inject
    @Named("solver1ScoreManager")
    ScoreManager<TestdataQuarkusSolution, HardMediumSoftLongScore> hardMediumSoftLongScoreManager1;
    @Inject
    @Named("solver1ScoreManager")
    ScoreManager<TestdataQuarkusSolution, HardMediumSoftBigDecimalScore> hardMediumSoftBigDecimalScoreManager1;
    @Inject
    @Named("solver1ScoreManager")
    ScoreManager<TestdataQuarkusSolution, BendableScore> bendableScoreManager1;
    @Inject
    @Named("solver1ScoreManager")
    ScoreManager<TestdataQuarkusSolution, BendableLongScore> bendableLongScoreManager1;
    @Inject
    @Named("solver1ScoreManager")
    ScoreManager<TestdataQuarkusSolution, BendableBigDecimalScore> bendableBigDecimalScoreManager1;

    // SolutionManager per score type
    @Inject
    @Named("solver1SolutionManager")
    SolutionManager<TestdataQuarkusSolution, SimpleScore> simpleSolutionManager1;
    @Inject
    @Named("solver1SolutionManager")
    SolutionManager<TestdataQuarkusSolution, SimpleLongScore> simpleLongSolutionManager1;
    @Inject
    @Named("solver1SolutionManager")
    SolutionManager<TestdataQuarkusSolution, SimpleBigDecimalScore> simpleBigDecimalSolutionManager1;
    @Inject
    @Named("solver1SolutionManager")
    SolutionManager<TestdataQuarkusSolution, HardSoftScore> hardSoftSolutionManager1;
    @Inject
    @Named("solver1SolutionManager")
    SolutionManager<TestdataQuarkusSolution, HardSoftLongScore> hardSoftLongSolutionManager1;
    @Inject
    @Named("solver1SolutionManager")
    SolutionManager<TestdataQuarkusSolution, HardSoftBigDecimalScore> hardSoftBigDecimalSolutionManager1;
    @Inject
    @Named("solver1SolutionManager")
    SolutionManager<TestdataQuarkusSolution, HardMediumSoftScore> hardMediumSoftSolutionManager1;
    @Inject
    @Named("solver1SolutionManager")
    SolutionManager<TestdataQuarkusSolution, HardMediumSoftLongScore> hardMediumSoftLongSolutionManager1;
    @Inject
    @Named("solver1SolutionManager")
    SolutionManager<TestdataQuarkusSolution, HardMediumSoftBigDecimalScore> hardMediumSoftBigDecimalSolutionManager1;
    @Inject
    @Named("solver1SolutionManager")
    SolutionManager<TestdataQuarkusSolution, BendableScore> bendableSolutionManager1;
    @Inject
    @Named("solver1SolutionManager")
    SolutionManager<TestdataQuarkusSolution, BendableLongScore> bendableLongSolutionManager1;
    @Inject
    @Named("solver1SolutionManager")
    SolutionManager<TestdataQuarkusSolution, BendableBigDecimalScore> bendableBigDecimalSolutionManager1;

    @Inject
    @Named("solver2Config")
    SolverConfig solver2Config;
    @Inject
    @Named("solver2Factory")
    SolverFactory<TestdataQuarkusSolution> solver2Factory;
    @Inject
    @Named("solver2Manager")
    SolverManager<TestdataQuarkusSolution, String> solver2Manager;

    // ScoreManager per score type
    @Inject
    @Named("solver2ScoreManager")
    ScoreManager<TestdataQuarkusSolution, SimpleScore> simpleScoreManager2;
    @Inject
    @Named("solver2ScoreManager")
    ScoreManager<TestdataQuarkusSolution, SimpleLongScore> simpleLongScoreManager2;
    @Inject
    @Named("solver2ScoreManager")
    ScoreManager<TestdataQuarkusSolution, SimpleBigDecimalScore> simpleBigDecimalScoreManager2;
    @Inject
    @Named("solver2ScoreManager")
    ScoreManager<TestdataQuarkusSolution, HardSoftScore> hardSoftScoreManager2;
    @Inject
    @Named("solver2ScoreManager")
    ScoreManager<TestdataQuarkusSolution, HardSoftLongScore> hardSoftLongScoreManager2;
    @Inject
    @Named("solver2ScoreManager")
    ScoreManager<TestdataQuarkusSolution, HardSoftBigDecimalScore> hardSoftBigDecimalScoreManager2;
    @Inject
    @Named("solver2ScoreManager")
    ScoreManager<TestdataQuarkusSolution, HardMediumSoftScore> hardMediumSoftScoreManager2;
    @Inject
    @Named("solver2ScoreManager")
    ScoreManager<TestdataQuarkusSolution, HardMediumSoftLongScore> hardMediumSoftLongScoreManager2;
    @Inject
    @Named("solver2ScoreManager")
    ScoreManager<TestdataQuarkusSolution, HardMediumSoftBigDecimalScore> hardMediumSoftBigDecimalScoreManager2;
    @Inject
    @Named("solver2ScoreManager")
    ScoreManager<TestdataQuarkusSolution, BendableScore> bendableScoreManager2;
    @Inject
    @Named("solver2ScoreManager")
    ScoreManager<TestdataQuarkusSolution, BendableLongScore> bendableLongScoreManager2;
    @Inject
    @Named("solver2ScoreManager")
    ScoreManager<TestdataQuarkusSolution, BendableBigDecimalScore> bendableBigDecimalScoreManager2;

    // SolutionManager per score type
    @Inject
    @Named("solver2SolutionManager")
    SolutionManager<TestdataQuarkusSolution, SimpleScore> simpleSolutionManager2;
    @Inject
    @Named("solver2SolutionManager")
    SolutionManager<TestdataQuarkusSolution, SimpleLongScore> simpleLongSolutionManager2;
    @Inject
    @Named("solver2SolutionManager")
    SolutionManager<TestdataQuarkusSolution, SimpleBigDecimalScore> simpleBigDecimalSolutionManager2;
    @Inject
    @Named("solver2SolutionManager")
    SolutionManager<TestdataQuarkusSolution, HardSoftScore> hardSoftSolutionManager2;
    @Inject
    @Named("solver2SolutionManager")
    SolutionManager<TestdataQuarkusSolution, HardSoftLongScore> hardSoftLongSolutionManager2;
    @Inject
    @Named("solver2SolutionManager")
    SolutionManager<TestdataQuarkusSolution, HardSoftBigDecimalScore> hardSoftBigDecimalSolutionManager2;
    @Inject
    @Named("solver2SolutionManager")
    SolutionManager<TestdataQuarkusSolution, HardMediumSoftScore> hardMediumSoftSolutionManager2;
    @Inject
    @Named("solver2SolutionManager")
    SolutionManager<TestdataQuarkusSolution, HardMediumSoftLongScore> hardMediumSoftLongSolutionManager2;
    @Inject
    @Named("solver2SolutionManager")
    SolutionManager<TestdataQuarkusSolution, HardMediumSoftBigDecimalScore> hardMediumSoftBigDecimalSolutionManager2;
    @Inject
    @Named("solver2SolutionManager")
    SolutionManager<TestdataQuarkusSolution, BendableScore> bendableSolutionManager2;
    @Inject
    @Named("solver2SolutionManager")
    SolutionManager<TestdataQuarkusSolution, BendableLongScore> bendableLongSolutionManager2;
    @Inject
    @Named("solver2SolutionManager")
    SolutionManager<TestdataQuarkusSolution, BendableBigDecimalScore> bendableBigDecimalSolutionManager2;

    @Test
    void solverProperties() {
        // solver1
        assertEquals(EnvironmentMode.FULL_ASSERT, solverConfig.getEnvironmentMode());
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

        // solver2
        assertNull(solver2Config.getEnvironmentMode());
        assertFalse(solver2Config.getDaemon());
        assertNull(solver2Config.getMoveThreadCount());
        assertEquals(DomainAccessType.REFLECTION, solver2Config.getDomainAccessType());
        assertEquals(Duration.ofHours(1), solver2Config.getTerminationConfig().getSpentLimit());
        assertNotNull(solver2Factory);
        assertNotNull(solver2Manager);
        assertNotSame(solver1Factory, solver2Factory);
        assertNotSame(solver1Manager, solver2Manager);
        // ScoreManager
        assertNotNull(simpleScoreManager2);
        assertNotNull(simpleLongScoreManager2);
        assertNotNull(simpleBigDecimalScoreManager2);
        assertNotNull(hardSoftScoreManager2);
        assertNotNull(hardSoftLongScoreManager2);
        assertNotNull(hardSoftBigDecimalScoreManager2);
        assertNotNull(hardMediumSoftScoreManager2);
        assertNotNull(hardMediumSoftLongScoreManager2);
        assertNotNull(hardMediumSoftBigDecimalScoreManager2);
        assertNotNull(bendableScoreManager2);
        assertNotNull(bendableLongScoreManager2);
        assertNotNull(bendableBigDecimalScoreManager2);
        // SolutionManager
        assertNotNull(simpleSolutionManager2);
        assertNotNull(simpleLongSolutionManager2);
        assertNotNull(simpleBigDecimalSolutionManager2);
        assertNotNull(hardSoftSolutionManager2);
        assertNotNull(hardSoftLongSolutionManager2);
        assertNotNull(hardSoftBigDecimalSolutionManager2);
        assertNotNull(hardMediumSoftSolutionManager2);
        assertNotNull(hardMediumSoftLongSolutionManager2);
        assertNotNull(hardMediumSoftBigDecimalSolutionManager2);
        assertNotNull(bendableSolutionManager2);
        assertNotNull(bendableLongSolutionManager2);
        assertNotNull(bendableBigDecimalSolutionManager2);
    }
}
