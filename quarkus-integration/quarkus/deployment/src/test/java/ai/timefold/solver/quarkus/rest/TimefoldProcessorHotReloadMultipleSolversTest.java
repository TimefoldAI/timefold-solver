package ai.timefold.solver.quarkus.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;

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

@SuppressWarnings("java:S5786") //The public modifier is required when using QuarkusDevModeTest extension.
public class TimefoldProcessorHotReloadMultipleSolversTest {

    @RegisterExtension
    static final QuarkusDevModeTest test = new QuarkusDevModeTest()
            .setBuildSystemProperty("quarkus.timefold.solver.\"solver1\".solver-config-xml",
                    "ai/timefold/solver/quarkus/customSolverQuarkusConfig.xml")
            .setBuildSystemProperty("quarkus.timefold.solver.\"solver2\".solver-config-xml",
                    "ai/timefold/solver/quarkus/customSolverQuarkusShadowVariableConfig.xml")
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(TestdataQuarkusEntity.class, TestdataQuarkusSolution.class,
                            TestdataQuarkusConstraintProvider.class)
                    .addClasses(TestdataQuarkusShadowVariableEntity.class,
                            TestdataQuarkusShadowVariableSolution.class,
                            TestdataQuarkusShadowVariableConstraintProvider.class,
                            TestdataQuarkusShadowVariableListener.class)
                    .addClasses(TestdataQuarkusShadowSolutionConfigResource.class)
                    .addAsResource("ai/timefold/solver/quarkus/customSolverQuarkusConfig.xml")
                    .addAsResource("ai/timefold/solver/quarkus/customSolverQuarkusShadowVariableConfig.xml"));

    @Test
    void solverConfigHotReload() {
        String resp = RestAssured.get("/solver-config/seconds-spent-limit").asString();
        assertEquals("secondsSpentLimit=0.50;secondsSpentLimit=0.12", resp);
        // First file
        test.modifyResourceFile("ai/timefold/solver/quarkus/customSolverQuarkusConfig.xml",
                s -> s.replace("<secondsSpentLimit>1</secondsSpentLimit>",
                        "<secondsSpentLimit>2</secondsSpentLimit>"));
        resp = RestAssured.get("/solver-config/seconds-spent-limit").asString();
        assertEquals("secondsSpentLimit=0.25;secondsSpentLimit=0.12", resp);
        // Second file
        test.modifyResourceFile("ai/timefold/solver/quarkus/customSolverQuarkusShadowVariableConfig.xml",
                s -> s.replace("<secondsSpentLimit>4</secondsSpentLimit>",
                        "<secondsSpentLimit>8</secondsSpentLimit>"));
        resp = RestAssured.get("/solver-config/seconds-spent-limit").asString();
        assertEquals("secondsSpentLimit=0.25;secondsSpentLimit=0.06", resp);
    }

}
