package ai.timefold.solver.quarkus;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.inject.Inject;
import jakarta.inject.Named;

import ai.timefold.solver.core.api.solver.SolverManager;
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

class TimefoldProcessorNeighborhoodUnusedTest {

    private static final String XML = "ai/timefold/solver/quarkus/neighborhoodSinglePhaseLevelSolverConfig.xml";

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .overrideConfigKey("quarkus.timefold.solver.\"solver1\".solver-config-xml", XML)
            .overrideConfigKey("quarkus.timefold.solver.\"solver2\".solver-config-xml", XML)
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(TestdataQuarkusEntity.class, TestdataQuarkusSolution.class,
                            TestdataQuarkusConstraintProvider.class)
                    .addClasses(TestdataQuarkusNeighborhoodProvider.class, OtherQuarkusNeighborhoodProvider.class,
                            TestdataQuarkusMoveSelectionStrategy.class)
                    .addAsResource(XML))
            .assertException(t -> assertThat(t)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Unused classes")
                    .hasMessageContaining(OtherQuarkusNeighborhoodProvider.class.getName())
                    .hasMessageContaining("NeighborhoodProvider"));

    @Inject
    @Named("solver1")
    SolverManager<TestdataQuarkusSolution> solverManager1;
    @Inject
    @Named("solver2")
    SolverManager<TestdataQuarkusSolution> solverManager2;

    @Test
    void test() {
        throw new IllegalStateException("The build should have failed.");
    }

}
