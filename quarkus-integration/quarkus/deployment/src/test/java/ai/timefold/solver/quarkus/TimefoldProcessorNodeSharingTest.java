package ai.timefold.solver.quarkus;

import static org.junit.jupiter.api.Assertions.assertEquals;

import jakarta.inject.Inject;

import ai.timefold.solver.core.config.solver.SolverConfig;
import ai.timefold.solver.quarkus.testdomain.normal.TestdataQuarkusConstraintProvider;
import ai.timefold.solver.quarkus.testdomain.normal.TestdataQuarkusEntity;
import ai.timefold.solver.quarkus.testdomain.normal.TestdataQuarkusSolution;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class TimefoldProcessorNodeSharingTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .overrideConfigKey("quarkus.timefold.solver-config-xml",
                    "ai/timefold/solver/quarkus/solverConfigWithNodeSharing.xml")
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(TestdataQuarkusEntity.class,
                            TestdataQuarkusSolution.class, TestdataQuarkusConstraintProvider.class)
                    .addAsResource("ai/timefold/solver/quarkus/solverConfigWithNodeSharing.xml"));

    @Inject
    SolverConfig solverConfig;

    @Test
    void isEnabledInSolverConfig() {
        assertEquals(true,
                solverConfig.getScoreDirectorFactoryConfig().getConstraintStreamAutomaticNodeSharing());
    }

}
