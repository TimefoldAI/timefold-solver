package ai.timefold.solver.benchmark.quarkus;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.inject.Inject;

import ai.timefold.solver.benchmark.config.PlannerBenchmarkConfig;
import ai.timefold.solver.benchmark.config.SolverBenchmarkConfig;
import ai.timefold.solver.benchmark.quarkus.testdata.normal.constraints.TestdataQuarkusConstraintProvider;
import ai.timefold.solver.benchmark.quarkus.testdata.normal.domain.TestdataQuarkusEntity;
import ai.timefold.solver.benchmark.quarkus.testdata.normal.domain.TestdataQuarkusOtherEntity;
import ai.timefold.solver.benchmark.quarkus.testdata.normal.domain.TestdataQuarkusSolution;
import ai.timefold.solver.core.config.solver.SolverConfig;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

class TimefoldBenchmarkProcessorInheritedSolverBenchmarkTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .overrideConfigKey("quarkus.timefold.benchmark.solver.termination.best-score-limit", "0")
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(TestdataQuarkusEntity.class, TestdataQuarkusOtherEntity.class,
                            TestdataQuarkusSolution.class, TestdataQuarkusConstraintProvider.class)
                    .addAsResource("solverConfig.xml")
                    .addAsResource("solverBenchmarkConfigWithInheritedSolverBenchmark.xml", "solverBenchmarkConfig.xml"));

    @Inject
    SolverConfig solverConfig;

    @Inject
    PlannerBenchmarkConfig plannerBenchmarkConfig;

    @Test
    void inheritClassesFromSolverConfig() {
        assertThat(solverConfig.getSolutionClass()).isEqualTo(TestdataQuarkusSolution.class);
        assertThat(solverConfig.getEntityClassList()).hasSize(2);
        assertThat(solverConfig.getEntityClassList()).contains(TestdataQuarkusEntity.class,
                TestdataQuarkusOtherEntity.class);
        assertThat(plannerBenchmarkConfig.getInheritedSolverBenchmarkConfig()
                .getSolverConfig().getTerminationConfig().getMillisecondsSpentLimit()).isEqualTo(5);
        assertThat(plannerBenchmarkConfig.getInheritedSolverBenchmarkConfig().getSolverConfig().getEntityClassList())
                .containsExactly(TestdataQuarkusEntity.class);

        SolverBenchmarkConfig childBenchmarkConfig = plannerBenchmarkConfig.getSolverBenchmarkConfigList().get(0);
        assertThat(childBenchmarkConfig.getSolverConfig().getSolutionClass()).isEqualTo(TestdataQuarkusSolution.class);
        assertThat(childBenchmarkConfig.getSolverConfig().getEntityClassList()).isNull(); // inherited from inherited solver config
        assertThat(childBenchmarkConfig.getSolverConfig().getScoreDirectorFactoryConfig()
                .getConstraintProviderClass()).isEqualTo(TestdataQuarkusConstraintProvider.class);

        childBenchmarkConfig = plannerBenchmarkConfig.getSolverBenchmarkConfigList().get(1);
        assertThat(childBenchmarkConfig.getSolverConfig().getSolutionClass())
                .isEqualTo(TestdataQuarkusConstraintProvider.class);
        assertThat(childBenchmarkConfig.getSolverConfig().getEntityClassList()).isNull(); // inherited from inherited solver config
        assertThat(childBenchmarkConfig.getSolverConfig().getScoreDirectorFactoryConfig()
                .getConstraintProviderClass()).isEqualTo(TestdataQuarkusConstraintProvider.class);
    }

}
