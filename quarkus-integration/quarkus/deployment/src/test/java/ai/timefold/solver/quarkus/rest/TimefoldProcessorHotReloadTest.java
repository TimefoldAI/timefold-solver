package ai.timefold.solver.quarkus.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;

import ai.timefold.solver.quarkus.testdata.normal.constraints.TestdataQuarkusConstraintProvider;
import ai.timefold.solver.quarkus.testdata.normal.domain.TestdataQuarkusEntity;
import ai.timefold.solver.quarkus.testdata.normal.domain.TestdataQuarkusSolution;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusDevModeTest;
import io.restassured.RestAssured;

/**
 * Do not remove the public modifier from this class. Otherwise, the test will fail with the following:
 * <p>
 * Caused by: java.lang.IllegalAccessException: class io.quarkus.test.QuarkusDevModeTest cannot access a member of class
 * ai.timefold.solver.quarkus.rest.TimefoldProcessorHotReloadTest with modifiers ""
 * at java.base/java.lang.reflect.AccessibleObject.checkAccess(AccessibleObject.java:674)
 * at java.base/java.lang.reflect.Constructor.newInstanceWithCaller(Constructor.java:489)
 * at java.base/java.lang.reflect.Constructor.newInstance(Constructor.java:480)
 * at io.quarkus.test.QuarkusDevModeTest.createTestInstance(QuarkusDevModeTest.java:222)
 */
@SuppressWarnings("java:S5786")
public class TimefoldProcessorHotReloadTest {

    @RegisterExtension
    static final QuarkusDevModeTest test = new QuarkusDevModeTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(TestdataQuarkusEntity.class,
                            TestdataQuarkusSolution.class, TestdataQuarkusConstraintProvider.class,
                            SolverConfigTestResource.class)
                    .addAsResource("solverConfig.xml"));

    @Test
    void solverConfigHotReload() {
        String resp = RestAssured.get("/solver-config/seconds-spent-limit").asString();
        assertEquals("secondsSpentLimit=2", resp);
        test.modifyResourceFile("solverConfig.xml", s -> s.replace("<secondsSpentLimit>2</secondsSpentLimit>",
                "<secondsSpentLimit>9</secondsSpentLimit>"));
        resp = RestAssured.get("/solver-config/seconds-spent-limit").asString();
        assertEquals("secondsSpentLimit=9", resp);
    }

}
