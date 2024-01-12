package ai.timefold.solver.quarkus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import jakarta.inject.Inject;
import jakarta.inject.Named;

import ai.timefold.solver.core.api.solver.SolverFactory;
import ai.timefold.solver.core.api.solver.SolverManager;
import ai.timefold.solver.core.config.solver.EnvironmentMode;
import ai.timefold.solver.core.config.solver.SolverConfig;
import ai.timefold.solver.quarkus.testdata.normal.constraints.TestdataQuarkusConstraintProvider;
import ai.timefold.solver.quarkus.testdata.normal.domain.TestdataQuarkusEntity;
import ai.timefold.solver.quarkus.testdata.normal.domain.TestdataQuarkusSolution;
import ai.timefold.solver.quarkus.testdata.shadowvariable.constraints.TestdataQuarkusShadowVariableConstraintProvider;
import ai.timefold.solver.quarkus.testdata.shadowvariable.domain.TestdataQuarkusShadowVariableEntity;
import ai.timefold.solver.quarkus.testdata.shadowvariable.domain.TestdataQuarkusShadowVariableListener;
import ai.timefold.solver.quarkus.testdata.shadowvariable.domain.TestdataQuarkusShadowVariableSolution;
import ai.timefold.solver.test.api.score.stream.ConstraintVerifier;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

class TimefoldProcessorMultipleSolverXmlTest {

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
                            TestdataQuarkusShadowVariableListener.class)
                    .addAsResource("ai/timefold/solver/quarkus/customSolver1Config.xml")
                    .addAsResource("ai/timefold/solver/quarkus/customSolver2Config.xml"));

    @Inject
    @Named("solver1Config")
    SolverConfig solver1Config;
    @Inject
    @Named("solver1Factory")
    SolverFactory<TestdataQuarkusSolution> solver1Factory;
    @Inject
    @Named("solver1Manager")
    SolverManager<TestdataQuarkusSolution, String> solver1Manager;
    @Inject
    ConstraintVerifier<TestdataQuarkusConstraintProvider, TestdataQuarkusSolution> solver1ConstraintVerifier;

    @Inject
    @Named("solver2Config")
    SolverConfig solver2Config;
    @Inject
    @Named("solver2Factory")
    SolverFactory<TestdataQuarkusShadowVariableSolution> solver2Factory;
    @Inject
    @Named("solver2Manager")
    SolverManager<TestdataQuarkusShadowVariableSolution, String> solver2Manager;
    @Inject
    ConstraintVerifier<TestdataQuarkusShadowVariableConstraintProvider, TestdataQuarkusShadowVariableSolution> solver2ConstraintVerifier;

    @Test
    void solverProperties() {
        // solver1
        assertEquals(EnvironmentMode.FULL_ASSERT, solver1Config.getEnvironmentMode());
        assertEquals(1L, solver1Config.getTerminationConfig().getSecondsSpentLimit());
        assertNotNull(solver1Factory);
        assertNotNull(solver1Manager);
        assertNotNull(solver1ConstraintVerifier);

        // solver2
        assertEquals(EnvironmentMode.REPRODUCIBLE, solver2Config.getEnvironmentMode());
        assertEquals(4L, solver2Config.getTerminationConfig().getSecondsSpentLimit());
        assertNotNull(solver2Factory);
        assertNotNull(solver2Manager);
        assertNotNull(solver2ConstraintVerifier);

        // Constraint providers
        assertNotEquals(solver1Config.getScoreDirectorFactoryConfig().getConstraintProviderClass().getName(),
                solver2Config.getScoreDirectorFactoryConfig().getConstraintProviderClass().getName());
    }
}
