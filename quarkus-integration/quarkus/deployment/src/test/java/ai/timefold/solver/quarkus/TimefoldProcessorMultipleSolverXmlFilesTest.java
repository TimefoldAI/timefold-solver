package ai.timefold.solver.quarkus;

import static org.junit.jupiter.api.Assertions.assertEquals;

import ai.timefold.solver.quarkus.rest.TestSolverConfigTestMultipleSolutionsResource;
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

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

class TimefoldProcessorMultipleSolverXmlFilesTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .overrideConfigKey("quarkus.timefold.solver.\"solver1\".environment-mode", "FULL_ASSERT")
            .overrideConfigKey("quarkus.timefold.solver.\"solver1\".solver-config-xml",
                    "ai/timefold/solver/quarkus/customSolver1Config.xml")
            .overrideConfigKey("quarkus.timefold.solver.\"solver2\".environment-mode", "REPRODUCIBLE")
            .overrideConfigKey("quarkus.timefold.solver.\"solver2\".solver-config-xml",
                    "ai/timefold/solver/quarkus/customSolver2Config.xml")
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(TestdataQuarkusEntity.class, TestdataQuarkusSolution.class,
                            TestdataQuarkusConstraintProvider.class)
                    .addClasses(TestdataQuarkusShadowVariableEntity.class,
                            TestdataQuarkusShadowVariableSolution.class,
                            TestdataQuarkusShadowVariableConstraintProvider.class,
                            TestdataQuarkusShadowVariableListener.class,
                            TestSolverConfigTestMultipleSolutionsResource.class)
                    .addAsResource("ai/timefold/solver/quarkus/customSolver1Config.xml")
                    .addAsResource("ai/timefold/solver/quarkus/customSolver2Config.xml"));

    @Test
    void solverProperties() {
        String resp = RestAssured.get("/solver-config/seconds-spent-limit").asString();
        assertEquals("secondsSpentLimit=0.50;secondsSpentLimit=0.12", resp);
    }
}
