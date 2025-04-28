
package ai.timefold.solver.quarkus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

import ai.timefold.solver.quarkus.testdomain.normal.constraints.TestdataQuarkusConstraintProvider;
import ai.timefold.solver.quarkus.testdomain.normal.domain.TestdataQuarkusEntity;
import ai.timefold.solver.quarkus.testdomain.normal.domain.TestdataQuarkusSolution;
import ai.timefold.solver.quarkus.testdomain.shadowvariable.domain.TestdataQuarkusShadowVariableSolution;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

class TimefoldProcessorSolverInvalidSolutionClassTest {

    // Empty classes
    @RegisterExtension
    static final QuarkusUnitTest config1 = new QuarkusUnitTest()
            .overrideConfigKey("quarkus.timefold.solver.environment-mode", "FULL_ASSERT")
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class).addClasses(TestdataQuarkusEntity.class))
            .assertException(t -> assertThat(t)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining(
                            "No classes were found with a @PlanningSolution annotation."));

    // Multiple classes
    @RegisterExtension
    static final QuarkusUnitTest config2 = new QuarkusUnitTest()
            .overrideConfigKey("quarkus.timefold.solver.environment-mode", "FULL_ASSERT")
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(TestdataQuarkusEntity.class, TestdataQuarkusSolution.class,
                            TestdataQuarkusConstraintProvider.class)
                    .addClasses(TestdataQuarkusShadowVariableSolution.class))
            .assertException(t -> assertThat(t)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Multiple classes")
                    .hasMessageContaining(
                            "ai.timefold.solver.quarkus.testdomain.shadowvariable.domain.TestdataQuarkusShadowVariableSolution")
                    .hasMessageContaining("ai.timefold.solver.quarkus.testdomain.normal.domain.TestdataQuarkusSolution")
                    .hasMessageContaining("found in the classpath with a @PlanningSolution annotation."));

    @Test
    void test() {
        fail("Should not call this method.");
    }
}
