package ai.timefold.solver.benchmark.quarkus;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.inject.Inject;

import ai.timefold.solver.benchmark.config.PlannerBenchmarkConfig;
import ai.timefold.solver.benchmark.quarkus.testdata.normal.constraints.TestdataQuarkusConstraintProvider;
import ai.timefold.solver.benchmark.quarkus.testdata.normal.domain.TestdataQuarkusEntity;
import ai.timefold.solver.benchmark.quarkus.testdata.normal.domain.TestdataQuarkusSolution;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

class TimefoldBenchmarkProcessorPhasesTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .overrideConfigKey("quarkus.timefold.benchmark.solver.termination.best-score-limit", "0")
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(TestdataQuarkusEntity.class,
                            TestdataQuarkusSolution.class, TestdataQuarkusConstraintProvider.class)
                    .addAsResource("solverConfigWithPhases.xml", "solverConfig.xml")
                    .addAsResource("solverBenchmarkConfigWithPhases.xml", "solverBenchmarkConfig.xml"));

    @Inject
    PlannerBenchmarkConfig plannerBenchmarkConfig;

    @Test
    void doesNotInheritPhasesFromSolverConfig() {
        assertThat(plannerBenchmarkConfig.getSolverBenchmarkConfigList().get(0).getSolverConfig().getPhaseConfigList())
                .hasSize(2);
        assertThat(plannerBenchmarkConfig.getSolverBenchmarkConfigList().get(1).getSolverConfig().getPhaseConfigList())
                .hasSize(3);
    }

}
