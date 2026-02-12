
package ai.timefold.solver.quarkus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

import jakarta.inject.Inject;
import jakarta.inject.Named;

import ai.timefold.solver.core.api.solver.SolverManager;
import ai.timefold.solver.core.testdomain.shadow.basic.TestdataBasicVarSolution;
import ai.timefold.solver.quarkus.testdomain.normal.TestdataQuarkusConstraintProvider;
import ai.timefold.solver.quarkus.testdomain.normal.TestdataQuarkusEntity;
import ai.timefold.solver.quarkus.testdomain.normal.TestdataQuarkusSolution;
import ai.timefold.solver.quarkus.testdomain.shadowvariable.TestdataQuarkusShadowVariableConstraintProvider;
import ai.timefold.solver.quarkus.testdomain.shadowvariable.TestdataQuarkusShadowVariableEntity;
import ai.timefold.solver.quarkus.testdomain.shadowvariable.TestdataQuarkusShadowVariableSolution;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

class TimefoldProcessorMultipleSolversInvalidSolutionClassTest {

    // Empty classes
    @RegisterExtension
    static final QuarkusUnitTest config1 = new QuarkusUnitTest()
            .overrideConfigKey("quarkus.timefold.solver.\"solver1\".environment-mode", "FULL_ASSERT")
            .overrideConfigKey("quarkus.timefold.solver.\"solver2\".environment-mode", "PHASE_ASSERT")
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class).addClasses(TestdataQuarkusEntity.class))
            .assertException(t -> assertThat(t)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining(
                            "No classes were found with a @PlanningSolution annotation."));

    // Multiple classes
    @RegisterExtension
    static final QuarkusUnitTest config2 = new QuarkusUnitTest()
            .overrideConfigKey("quarkus.timefold.solver.\"solver1\".environment-mode", "FULL_ASSERT")
            .overrideConfigKey("quarkus.timefold.solver.\"solver2\".environment-mode", "PHASE_ASSERT")
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(TestdataQuarkusEntity.class, TestdataQuarkusSolution.class,
                            TestdataQuarkusConstraintProvider.class)
                    .addClasses(TestdataQuarkusShadowVariableEntity.class,
                            TestdataQuarkusShadowVariableSolution.class,
                            TestdataQuarkusShadowVariableConstraintProvider.class))
            .assertException(t -> assertThat(t)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Some solver configs")
                    .hasMessageContaining("solver1")
                    .hasMessageContaining("solver2")
                    .hasMessageContaining("don't specify a PlanningSolution class, yet there are multiple available")
                    .hasMessageContaining(
                            TestdataQuarkusShadowVariableSolution.class.getName())
                    .hasMessageContaining(TestdataQuarkusSolution.class.getName())
                    .hasMessageContaining("on the classpath."));

    // Unused classes
    @RegisterExtension
    static final QuarkusUnitTest config3 = new QuarkusUnitTest()
            .overrideConfigKey("quarkus.timefold.solver.\"solver1\".solver-config-xml",
                    "ai/timefold/solver/quarkus/customSolverQuarkusConfig.xml")
            .overrideConfigKey("quarkus.timefold.solver.\"solver2\".solver-config-xml",
                    "ai/timefold/solver/quarkus/customSolverQuarkusShadowVariableConfig.xml")
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(TestdataQuarkusEntity.class, TestdataQuarkusSolution.class,
                            TestdataQuarkusConstraintProvider.class)
                    .addClasses(TestdataQuarkusShadowVariableEntity.class,
                            TestdataQuarkusShadowVariableSolution.class,
                            TestdataQuarkusShadowVariableConstraintProvider.class)
                    .addClasses(TestdataBasicVarSolution.class)
                    .addAsResource("ai/timefold/solver/quarkus/customSolverQuarkusConfig.xml")
                    .addAsResource("ai/timefold/solver/quarkus/customSolverQuarkusShadowVariableConfig.xml"))
            .assertException(t -> assertThat(t)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining(
                            "Unused classes ([%s]) found with a @PlanningSolution annotation."
                                    .formatted(TestdataBasicVarSolution.class.getName())));

    @Inject
    @Named("solver1")
    SolverManager<?, ?> solverManager1;

    @Inject
    @Named("solver2")
    SolverManager<?, ?> solverManager2;

    @Test
    void test() {
        fail("Should not call this method.");
    }
}
