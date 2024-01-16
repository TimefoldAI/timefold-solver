package ai.timefold.solver.quarkus.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import ai.timefold.solver.core.api.solver.SolverManager;
import ai.timefold.solver.core.impl.solver.DefaultSolverJob;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;
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
        @Named("solver1")
        SolverManager<TestdataQuarkusSolution, Long> solver1;

        @Inject
        @Named("solver2")
        SolverManager<TestdataQuarkusShadowVariableSolution, Long> solver2;

        private static long count = 0;

        @GET
        @Path("/seconds-spent-limit")
        @Produces(MediaType.TEXT_PLAIN)
        public String secondsSpentLimit() {
            DecimalFormatSymbols decimalFormatSymbols = DecimalFormatSymbols.getInstance();
            decimalFormatSymbols.setDecimalSeparator('.');
            DecimalFormat decimalFormat = new DecimalFormat("0.00", decimalFormatSymbols);

            // Solver 1
            SolverScope<TestdataQuarkusSolution> solverScopeSolver1 = mock(SolverScope.class);
            doReturn(500L).when(solverScopeSolver1).calculateTimeMillisSpentUpToNow();
            DefaultSolverJob<TestdataQuarkusSolution, Long> jobSolver1 =
                    (DefaultSolverJob<TestdataQuarkusSolution, Long>) solver1.solve(++count, new TestdataQuarkusSolution());
            double gradientTimeSolver1 = jobSolver1.getSolverTermination().calculateSolverTimeGradient(solverScopeSolver1);

            // Solver 2
            SolverScope<TestdataQuarkusShadowVariableSolution> solverScopeSolver2 = mock(SolverScope.class);
            doReturn(500L).when(solverScopeSolver2).calculateTimeMillisSpentUpToNow();
            DefaultSolverJob<TestdataQuarkusShadowVariableSolution, Long> jobSolver2 =
                    (DefaultSolverJob<TestdataQuarkusShadowVariableSolution, Long>) solver2.solve(++count,
                            new TestdataQuarkusShadowVariableSolution());
            double gradientTimeSolver2 = jobSolver2.getSolverTermination().calculateSolverTimeGradient(solverScopeSolver2);

            return String.format("secondsSpentLimit=%s;secondsSpentLimit=%s", decimalFormat.format(gradientTimeSolver1),
                    decimalFormat.format(gradientTimeSolver2));
        }
    }

    @Test
    void solverConfigHotReload() {
        String resp = RestAssured.get("/solver-config/seconds-spent-limit").asString();
        assertEquals("secondsSpentLimit=0.50;secondsSpentLimit=0.12", resp);
        // First file
        test.modifyResourceFile("ai/timefold/solver/quarkus/customSolver1Config.xml",
                s -> s.replace("<secondsSpentLimit>1</secondsSpentLimit>",
                        "<secondsSpentLimit>2</secondsSpentLimit>"));
        resp = RestAssured.get("/solver-config/seconds-spent-limit").asString();
        assertEquals("secondsSpentLimit=0.25;secondsSpentLimit=0.12", resp);
        // Second file
        test.modifyResourceFile("ai/timefold/solver/quarkus/customSolver2Config.xml",
                s -> s.replace("<secondsSpentLimit>4</secondsSpentLimit>",
                        "<secondsSpentLimit>8</secondsSpentLimit>"));
        resp = RestAssured.get("/solver-config/seconds-spent-limit").asString();
        assertEquals("secondsSpentLimit=0.25;secondsSpentLimit=0.06", resp);
    }

}
