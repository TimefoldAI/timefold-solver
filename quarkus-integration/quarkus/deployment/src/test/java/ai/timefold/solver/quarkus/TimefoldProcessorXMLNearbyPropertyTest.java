package ai.timefold.solver.quarkus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Collections;

import jakarta.inject.Inject;

import ai.timefold.solver.core.api.solver.SolverFactory;
import ai.timefold.solver.core.config.solver.SolverConfig;
import ai.timefold.solver.quarkus.testdomain.dummy.DummyDistanceMeter;
import ai.timefold.solver.quarkus.testdomain.normal.TestdataQuarkusConstraintProvider;
import ai.timefold.solver.quarkus.testdomain.normal.TestdataQuarkusEntity;
import ai.timefold.solver.quarkus.testdomain.normal.TestdataQuarkusSolution;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

class TimefoldProcessorXMLNearbyPropertyTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .overrideConfigKey("quarkus.timefold.solver-config-xml", "ai/timefold/solver/quarkus/customNearbySolverConfig.xml")
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(TestdataQuarkusEntity.class, TestdataQuarkusSolution.class,
                            TestdataQuarkusConstraintProvider.class, DummyDistanceMeter.class)
                    .addAsResource("ai/timefold/solver/quarkus/customNearbySolverConfig.xml"));

    @Inject
    SolverConfig solverConfig;
    @Inject
    SolverFactory<TestdataQuarkusSolution> solverFactory;

    @Test
    void solverConfigXml_property() {
        assertNotNull(solverConfig);
        assertNotNull(solverConfig.getNearbyDistanceMeterClass());
        assertEquals(TestdataQuarkusSolution.class, solverConfig.getSolutionClass());
        assertEquals(Collections.singletonList(TestdataQuarkusEntity.class), solverConfig.getEntityClassList());
        assertEquals(TestdataQuarkusConstraintProvider.class,
                solverConfig.getScoreDirectorFactoryConfig().getConstraintProviderClass());
        // Properties defined in solverConfig.xml
        assertEquals(3L, solverConfig.getTerminationConfig().getSecondsSpentLimit().longValue());
        assertNotNull(solverFactory);
    }

}
