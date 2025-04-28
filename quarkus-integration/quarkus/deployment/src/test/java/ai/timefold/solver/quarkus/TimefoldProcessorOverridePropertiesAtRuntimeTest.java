package ai.timefold.solver.quarkus;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import ai.timefold.solver.core.config.solver.SolverConfig;
import ai.timefold.solver.core.config.solver.SolverManagerConfig;
import ai.timefold.solver.quarkus.testdomain.normal.constraints.TestdataQuarkusConstraintProvider;
import ai.timefold.solver.quarkus.testdomain.normal.domain.TestdataQuarkusEntity;
import ai.timefold.solver.quarkus.testdomain.normal.domain.TestdataQuarkusSolution;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.bootstrap.model.AppArtifact;
import io.quarkus.test.QuarkusProdModeTest;
import io.restassured.RestAssured;

class TimefoldProcessorOverridePropertiesAtRuntimeTest {

    private static final String QUARKUS_VERSION = getRequiredProperty("version.io.quarkus");

    private static String getRequiredProperty(String name) {
        final String v = System.getProperty(name);
        if (v == null || v.isEmpty()) {
            throw new IllegalStateException("The system property (" + name + ") has not been set.");
        }
        return v;
    }

    @RegisterExtension
    static final QuarkusProdModeTest config = new QuarkusProdModeTest()
            .setForcedDependencies(List.of(new AppArtifact("io.quarkus", "quarkus-rest", QUARKUS_VERSION)))
            // We want to check if these are overridden at runtime
            .overrideConfigKey("quarkus.timefold.solver.termination.best-score-limit", "0")
            .overrideConfigKey("quarkus.timefold.solver.move-thread-count", "4")
            .overrideConfigKey("quarkus.timefold.solver-manager.parallel-solver-count", "1")
            .overrideConfigKey("quarkus.timefold.solver.termination.diminished-returns.enabled", "false")
            .overrideConfigKey("quarkus.timefold.solver.termination.diminished-returns.sliding-window-duration", "3h")
            .overrideConfigKey("quarkus.timefold.solver.termination.diminished-returns.minimum-improvement-ratio", "0.25")
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(TestdataQuarkusEntity.class,
                            TestdataQuarkusSolution.class,
                            TestdataQuarkusConstraintProvider.class,
                            TimefoldTestResource.class))
            .setRuntimeProperties(getRuntimeProperties())
            .setRun(true);

    private static Map<String, String> getRuntimeProperties() {
        Map<String, String> out = new HashMap<>();
        out.put("quarkus.timefold.solver.termination.best-score-limit", "7");
        out.put("quarkus.timefold.solver.move-thread-count", "3");
        out.put("quarkus.timefold.solver-manager.parallel-solver-count", "10");
        out.put("quarkus.timefold.solver.termination.diminished-returns.enabled", "true");
        out.put("quarkus.timefold.solver.termination.diminished-returns.sliding-window-duration", "6h");
        out.put("quarkus.timefold.solver.termination.diminished-returns.minimum-improvement-ratio", "0.5");
        return out;
    }

    // Can't use injection, so we need a resource to fetch the properties
    @Path("/timefold/test")
    public static class TimefoldTestResource {
        @Inject
        SolverConfig solverConfig;

        @Inject
        SolverManagerConfig solverManagerConfig;

        @GET
        @Path("/solver-config")
        @Produces(MediaType.TEXT_PLAIN)
        public String getSolverConfig() {
            var diminishedReturnsConfig = solverConfig.getTerminationConfig()
                    .getDiminishedReturnsConfig();
            return """
                    termination.diminished-returns.sliding-window-duration=%sh
                    termination.diminished-returns.minimum-improvement-ratio=%s
                    termination.bestScoreLimit=%s
                    moveThreadCount=%s
                    """
                    .formatted(diminishedReturnsConfig.getSlidingWindowDuration().toHours(),
                            diminishedReturnsConfig.getMinimumImprovementRatio(),
                            solverConfig.getTerminationConfig().getBestScoreLimit(),
                            solverConfig.getMoveThreadCount());
        }

        @GET
        @Path("/solver-manager-config")
        @Produces(MediaType.TEXT_PLAIN)
        public String getSolverManagerConfig() {
            StringBuilder sb = new StringBuilder();
            sb.append("parallelSolverCount=").append(solverManagerConfig.getParallelSolverCount()).append("\n");
            return sb.toString();
        }
    }

    @Test
    void solverConfigPropertiesShouldBeOverwritten() throws IOException {
        Properties solverConfigProperties = new Properties();
        solverConfigProperties.load(RestAssured.given()
                .contentType(MediaType.TEXT_PLAIN)
                .accept(MediaType.TEXT_PLAIN)
                .when()
                .get("/timefold/test/solver-config")
                .asInputStream());
        assertEquals("6h", solverConfigProperties.get("termination.diminished-returns.sliding-window-duration"));
        assertEquals("0.5", solverConfigProperties.get("termination.diminished-returns.minimum-improvement-ratio"));
        assertEquals("7", solverConfigProperties.get("termination.bestScoreLimit"));
        assertEquals("3", solverConfigProperties.get("moveThreadCount"));
    }

    @Test
    void solverManagerConfigPropertiesShouldBeOverwritten() throws IOException {
        Properties solverManagerProperties = new Properties();
        solverManagerProperties.load(RestAssured.given()
                .contentType(MediaType.TEXT_PLAIN)
                .accept(MediaType.TEXT_PLAIN)
                .when()
                .get("/timefold/test/solver-manager-config")
                .asInputStream());
        assertEquals("10", solverManagerProperties.get("parallelSolverCount"));
    }

}
