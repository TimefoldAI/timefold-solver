package ai.timefold.solver.quarkus;

import static org.assertj.core.api.Assertions.assertThat;

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
import ai.timefold.solver.core.api.score.stream.ConstraintMetaModel;
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
    ConstraintMetaModel constraintMetaModel;

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
        assertThat(constraintMetaModel).isNotNull();
        assertThat(constraintMetaModel.getConstraints())
                .isNotEmpty();

        // solver1
        assertThat((Object) solverConfig.getEnvironmentMode()).isEqualTo(EnvironmentMode.FULL_ASSERT);
        assertThat(solverConfig.getNearbyDistanceMeterClass()).isNull();
        assertThat(solverConfig.getDaemon()).isTrue();
        assertThat(solverConfig.getDomainAccessType()).isEqualTo(DomainAccessType.REFLECTION);
        assertThat(solverConfig.getScoreDirectorFactoryConfig().getConstraintStreamImplType()).isNull();
        assertThat(solver1Factory).isNotNull();
        assertThat(solverConfig.getTerminationConfig().getSpentLimit()).isEqualTo(Duration.ofHours(4));
        assertThat(solverConfig.getTerminationConfig().getUnimprovedSpentLimit()).isEqualTo(Duration.ofHours(5));
        assertThat(solverConfig.getTerminationConfig().getBestScoreLimit()).isEqualTo(SimpleScore.of(0).toString());
        assertThat(solver1Factory).isNotNull();
        assertThat(solver1Manager).isNotNull();
        // SolutionManager
        assertThat(simpleSolutionManager1).isNotNull();
        assertThat(simpleLongSolutionManager1).isNotNull();
        assertThat(simpleBigDecimalSolutionManager1).isNotNull();
        assertThat(hardSoftSolutionManager1).isNotNull();
        assertThat(hardSoftLongSolutionManager1).isNotNull();
        assertThat(hardSoftBigDecimalSolutionManager1).isNotNull();
        assertThat(hardMediumSoftSolutionManager1).isNotNull();
        assertThat(hardMediumSoftLongSolutionManager1).isNotNull();
        assertThat(hardMediumSoftBigDecimalSolutionManager1).isNotNull();
        assertThat(bendableSolutionManager1).isNotNull();
        assertThat(bendableLongSolutionManager1).isNotNull();
        assertThat(bendableBigDecimalSolutionManager1).isNotNull();
        // ScoreManager
        assertThat(simpleScoreManager1).isNotNull();
        assertThat(simpleLongScoreManager1).isNotNull();
        assertThat(simpleBigDecimalScoreManager1).isNotNull();
        assertThat(hardSoftScoreManager1).isNotNull();
        assertThat(hardSoftLongScoreManager1).isNotNull();
        assertThat(hardSoftBigDecimalScoreManager1).isNotNull();
        assertThat(hardMediumSoftScoreManager1).isNotNull();
        assertThat(hardMediumSoftLongScoreManager1).isNotNull();
        assertThat(hardMediumSoftBigDecimalScoreManager1).isNotNull();
        assertThat(bendableScoreManager1).isNotNull();
        assertThat(bendableLongScoreManager1).isNotNull();
        assertThat(bendableBigDecimalScoreManager1).isNotNull();
    }
}
