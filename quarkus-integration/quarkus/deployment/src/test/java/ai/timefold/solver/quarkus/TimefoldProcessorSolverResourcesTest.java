package ai.timefold.solver.quarkus;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import jakarta.inject.Inject;
import jakarta.inject.Named;

import ai.timefold.solver.core.api.score.BendableBigDecimalScore;
import ai.timefold.solver.core.api.score.BendableScore;
import ai.timefold.solver.core.api.score.HardMediumSoftBigDecimalScore;
import ai.timefold.solver.core.api.score.HardMediumSoftScore;
import ai.timefold.solver.core.api.score.HardSoftBigDecimalScore;
import ai.timefold.solver.core.api.score.HardSoftScore;
import ai.timefold.solver.core.api.score.SimpleBigDecimalScore;
import ai.timefold.solver.core.api.score.SimpleScore;
import ai.timefold.solver.core.api.score.stream.ConstraintMetaModel;
import ai.timefold.solver.core.api.solver.SolutionManager;
import ai.timefold.solver.core.api.solver.SolverFactory;
import ai.timefold.solver.core.api.solver.SolverManager;
import ai.timefold.solver.core.config.solver.EnvironmentMode;
import ai.timefold.solver.core.config.solver.SolverConfig;
import ai.timefold.solver.quarkus.testdomain.normal.TestdataQuarkusConstraintProvider;
import ai.timefold.solver.quarkus.testdomain.normal.TestdataQuarkusEntity;
import ai.timefold.solver.quarkus.testdomain.normal.TestdataQuarkusSolution;

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
    @Named("solver1")
    SolverManager<?, ?> solverManager1;

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
    SolutionManager<TestdataQuarkusSolution, SimpleBigDecimalScore> simpleBigDecimalSolutionManager1;
    @Inject
    SolutionManager<TestdataQuarkusSolution, HardSoftScore> hardSoftSolutionManager1;
    @Inject
    SolutionManager<TestdataQuarkusSolution, HardSoftBigDecimalScore> hardSoftBigDecimalSolutionManager1;
    @Inject
    SolutionManager<TestdataQuarkusSolution, HardMediumSoftScore> hardMediumSoftSolutionManager1;
    @Inject
    SolutionManager<TestdataQuarkusSolution, HardMediumSoftBigDecimalScore> hardMediumSoftBigDecimalSolutionManager1;
    @Inject
    SolutionManager<TestdataQuarkusSolution, BendableScore> bendableSolutionManager1;
    @Inject
    SolutionManager<TestdataQuarkusSolution, BendableBigDecimalScore> bendableBigDecimalSolutionManager1;

    @Test
    void solverProperties() {
        assertThat(constraintMetaModel).isNotNull();
        assertThat(constraintMetaModel.getConstraints())
                .isNotEmpty();

        // solver1
        assertThat((Object) solverConfig.getEnvironmentMode()).isEqualTo(EnvironmentMode.FULL_ASSERT);
        assertThat(solverConfig.getNearbyDistanceMeterClass()).isNull();
        assertThat(solverConfig.getDaemon()).isTrue();
        assertThat(solver1Factory).isNotNull();
        assertThat(solverConfig.getTerminationConfig().getSpentLimit()).isEqualTo(Duration.ofHours(4));
        assertThat(solverConfig.getTerminationConfig().getUnimprovedSpentLimit()).isEqualTo(Duration.ofHours(5));
        assertThat(solverConfig.getTerminationConfig().getBestScoreLimit()).isEqualTo(SimpleScore.of(0).toString());
        assertThat(solver1Factory).isNotNull();
        assertThat(solver1Manager).isNotNull();
        // SolutionManager
        assertThat(simpleSolutionManager1).isNotNull();
        assertThat(simpleBigDecimalSolutionManager1).isNotNull();
        assertThat(hardSoftSolutionManager1).isNotNull();
        assertThat(hardSoftBigDecimalSolutionManager1).isNotNull();
        assertThat(hardMediumSoftSolutionManager1).isNotNull();
        assertThat(hardMediumSoftBigDecimalSolutionManager1).isNotNull();
        assertThat(bendableSolutionManager1).isNotNull();
        assertThat(bendableBigDecimalSolutionManager1).isNotNull();
    }
}
