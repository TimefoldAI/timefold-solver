package ai.timefold.solver.quarkus;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.inject.Inject;

import ai.timefold.solver.core.config.solver.SolverConfig;
import ai.timefold.solver.quarkus.testdomain.neighborhood.OtherQuarkusMoveSelectionStrategy;
import ai.timefold.solver.quarkus.testdomain.neighborhood.OtherQuarkusNeighborhoodProvider;
import ai.timefold.solver.quarkus.testdomain.neighborhood.TestdataQuarkusMoveSelectionStrategy;
import ai.timefold.solver.quarkus.testdomain.neighborhood.TestdataQuarkusNeighborhoodProvider;
import ai.timefold.solver.quarkus.testdomain.normal.TestdataQuarkusConstraintProvider;
import ai.timefold.solver.quarkus.testdomain.normal.TestdataQuarkusEntity;
import ai.timefold.solver.quarkus.testdomain.normal.TestdataQuarkusSolution;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

class TimefoldProcessorNeighborhoodIgnoredTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .overrideConfigKey("quarkus.timefold.solver.termination.best-score-limit", "0")
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(TestdataQuarkusEntity.class, TestdataQuarkusSolution.class,
                            TestdataQuarkusConstraintProvider.class)
                    .addClasses(TestdataQuarkusNeighborhoodProvider.class, OtherQuarkusNeighborhoodProvider.class,
                            TestdataQuarkusMoveSelectionStrategy.class, OtherQuarkusMoveSelectionStrategy.class));

    @Inject
    SolverConfig solverConfig;

    @Test
    void ignoredWithoutNeighborhoods() {
        assertThat(solverConfig.getNeighborhoodProviderClass()).isNull();
        assertThat(solverConfig.getMoveSelectionStrategyClass()).isNull();
    }

}
