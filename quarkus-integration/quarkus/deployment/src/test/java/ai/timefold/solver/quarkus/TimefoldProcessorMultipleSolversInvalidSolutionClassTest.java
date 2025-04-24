
package ai.timefold.solver.quarkus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

import jakarta.inject.Inject;
import jakarta.inject.Named;

import ai.timefold.solver.core.api.solver.SolverManager;
import ai.timefold.solver.quarkus.testdata.chained.domain.TestdataChainedQuarkusSolution;
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
                            TestdataQuarkusShadowVariableConstraintProvider.class,
                            TestdataQuarkusShadowVariableListener.class))
            .assertException(t -> assertThat(t)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Some solver configs")
                    .hasMessageContaining("solver1")
                    .hasMessageContaining("solver2")
                    .hasMessageContaining("don't specify a PlanningSolution class, yet there are multiple available")
                    .hasMessageContaining(
                            "ai.timefold.solver.quarkus.testdata.shadowvariable.domain.TestdataQuarkusShadowVariableSolution")
                    .hasMessageContaining("ai.timefold.solver.quarkus.testdata.normal.domain.TestdataQuarkusSolution")
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
                            TestdataQuarkusShadowVariableConstraintProvider.class,
                            TestdataQuarkusShadowVariableListener.class)
                    .addClasses(TestdataChainedQuarkusSolution.class)
                    .addAsResource("ai/timefold/solver/quarkus/customSolverQuarkusConfig.xml")
                    .addAsResource("ai/timefold/solver/quarkus/customSolverQuarkusShadowVariableConfig.xml"))
            .assertException(t -> assertThat(t)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining(
                            "Unused classes ([ai.timefold.solver.quarkus.testdata.chained.domain.TestdataChainedQuarkusSolution]) found with a @PlanningSolution annotation."));

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
