package ai.timefold.solver.quarkus.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import ai.timefold.solver.core.config.solver.SolverConfig;
import ai.timefold.solver.quarkus.testdata.normal.constraints.TestdataQuarkusConstraintProvider;
import ai.timefold.solver.quarkus.testdata.normal.domain.TestdataQuarkusEntity;
import ai.timefold.solver.quarkus.testdata.normal.domain.TestdataQuarkusSolution;
import ai.timefold.solver.quarkus.testdata.shadowvariable.constraints.TestdataQuarkusShadowVariableConstraintProvider;
import ai.timefold.solver.quarkus.testdata.shadowvariable.domain.TestdataQuarkusShadowVariableEntity;
import ai.timefold.solver.quarkus.testdata.shadowvariable.domain.TestdataQuarkusShadowVariableListener;
import ai.timefold.solver.quarkus.testdata.shadowvariable.domain.TestdataQuarkusShadowVariableSolution;

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
 * ai.timefold.solver.quarkus.rest.TimefoldProcessorHotReloadMultipleSolversTest with modifiers ""
 * at java.base/java.lang.reflect.AccessibleObject.checkAccess(AccessibleObject.java:674)
 * at java.base/java.lang.reflect.Constructor.newInstanceWithCaller(Constructor.java:489)
 * at java.base/java.lang.reflect.Constructor.newInstance(Constructor.java:480)
 * at io.quarkus.test.QuarkusDevModeTest.createTestInstance(QuarkusDevModeTest.java:222)
 */
@SuppressWarnings("java:S5786")
public class TimefoldProcessorHotReloadMultipleSolversTest {

    @RegisterExtension
    static final QuarkusDevModeTest test = new QuarkusDevModeTest()
            .setBuildSystemProperty("quarkus.timefold.solver.\"solver1\".solver-config-xml",
                    "ai/timefold/solver/quarkus/customSolver1Config.xml")
            .setBuildSystemProperty("quarkus.timefold.solver.\"solver2\".solver-config-xml",
                    "ai/timefold/solver/quarkus/customSolver2Config.xml")
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(TestdataQuarkusEntity.class, TestdataQuarkusSolution.class,
                            TestdataQuarkusConstraintProvider.class)
                    .addClasses(TestdataQuarkusShadowVariableEntity.class,
                            TestdataQuarkusShadowVariableSolution.class,
                            TestdataQuarkusShadowVariableConstraintProvider.class,
                            TestdataQuarkusShadowVariableListener.class)
                    .addClasses(TestSolverConfigTestResource.class)
                    .addAsResource("ai/timefold/solver/quarkus/customSolver1Config.xml")
                    .addAsResource("ai/timefold/solver/quarkus/customSolver2Config.xml"));

    @Path("/solver-config")
    @ApplicationScoped
    public static class TestSolverConfigTestResource {
        @Inject
        @Named("solver1Config")
        SolverConfig solverConfig;

        @Inject
        @Named("solver2Config")
        SolverConfig solver2Config;

        @GET
        @Path("/seconds-spent-limit")
        @Produces(MediaType.TEXT_PLAIN)
        public String secondsSpentLimit() {
            return "secondsSpentLimit=" + solverConfig.getTerminationConfig().getSecondsSpentLimit() + ";"
                    + "secondsSpentLimit=" + solver2Config.getTerminationConfig().getSecondsSpentLimit();
        }
    }

    @Test
    void solverConfigHotReload() {
        String resp = RestAssured.get("/solver-config/seconds-spent-limit").asString();
        assertEquals("secondsSpentLimit=1;secondsSpentLimit=4", resp);
        // First file
        test.modifyResourceFile("ai/timefold/solver/quarkus/customSolver1Config.xml",
                s -> s.replace("<secondsSpentLimit>1</secondsSpentLimit>",
                        "<secondsSpentLimit>9</secondsSpentLimit>"));
        resp = RestAssured.get("/solver-config/seconds-spent-limit").asString();
        assertEquals("secondsSpentLimit=9;secondsSpentLimit=4", resp);
        // Second file
        test.modifyResourceFile("ai/timefold/solver/quarkus/customSolver2Config.xml",
                s -> s.replace("<secondsSpentLimit>4</secondsSpentLimit>",
                        "<secondsSpentLimit>10</secondsSpentLimit>"));
        resp = RestAssured.get("/solver-config/seconds-spent-limit").asString();
        assertEquals("secondsSpentLimit=9;secondsSpentLimit=10", resp);
    }

}
