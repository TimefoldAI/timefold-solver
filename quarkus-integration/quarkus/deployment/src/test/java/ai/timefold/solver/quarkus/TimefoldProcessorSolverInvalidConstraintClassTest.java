
package ai.timefold.solver.quarkus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

import ai.timefold.solver.quarkus.testdata.dummy.DummyTestdataQuarkusEasyScoreCalculator;
import ai.timefold.solver.quarkus.testdata.dummy.DummyTestdataQuarkusIncrementalScoreCalculator;
import ai.timefold.solver.quarkus.testdata.dummy.DummyTestdataQuarkusShadowVariableEasyScoreCalculator;
import ai.timefold.solver.quarkus.testdata.dummy.DummyTestdataQuarkusShadowVariableIncrementalScoreCalculator;
import ai.timefold.solver.quarkus.testdata.normal.constraints.TestdataQuarkusConstraintProvider;
import ai.timefold.solver.quarkus.testdata.normal.domain.TestdataQuarkusEntity;
import ai.timefold.solver.quarkus.testdata.normal.domain.TestdataQuarkusSolution;
import ai.timefold.solver.quarkus.testdata.shadowvariable.constraints.TestdataQuarkusShadowVariableConstraintProvider;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

class TimefoldProcessorSolverInvalidConstraintClassTest {

    // Empty classes
    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .overrideConfigKey("quarkus.timefold.solver.environment-mode", "FULL_ASSERT")
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(TestdataQuarkusEntity.class, TestdataQuarkusSolution.class))
            .assertException(t -> assertThat(t)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining(
                            "No classes found that implement EasyScoreCalculator, ConstraintProvider, or IncrementalScoreCalculator."));

    // Multiple classes - EasyScoreCalculator
    @RegisterExtension
    static final QuarkusUnitTest config2 = new QuarkusUnitTest()
            .overrideConfigKey("quarkus.timefold.solver.environment-mode", "FULL_ASSERT")
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(TestdataQuarkusEntity.class, TestdataQuarkusSolution.class,
                            DummyTestdataQuarkusEasyScoreCalculator.class)
                    .addClasses(DummyTestdataQuarkusShadowVariableEasyScoreCalculator.class))
            .assertException(t -> assertThat(t)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Multiple score classes")
                    .hasMessageContaining(
                            "ai.timefold.solver.quarkus.testdata.dummy.DummyTestdataQuarkusEasyScoreCalculator")
                    .hasMessageContaining(
                            "ai.timefold.solver.quarkus.testdata.dummy.DummyTestdataQuarkusShadowVariableEasyScoreCalculator")
                    .hasMessageContaining("that implements EasyScoreCalculator were found"));

    // Multiple classes - EasyScoreCalculator with XML
    @RegisterExtension
    static final QuarkusUnitTest config3 = new QuarkusUnitTest()
            .overrideConfigKey("quarkus.timefold.solver.environment-mode", "FULL_ASSERT")
            .overrideConfigKey("quarkus.timefold.solver.solver-config-xml",
                    "ai/timefold/solver/quarkus/customSolverConfig.xml")
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(TestdataQuarkusEntity.class, TestdataQuarkusSolution.class)
                    .addClasses(DummyTestdataQuarkusEasyScoreCalculator.class,
                            DummyTestdataQuarkusShadowVariableEasyScoreCalculator.class)
                    .addAsResource("ai/timefold/solver/quarkus/customSolverConfig.xml"))
            .assertException(t -> assertThat(t)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Multiple score classes")
                    .hasMessageContaining(
                            "ai.timefold.solver.quarkus.testdata.dummy.DummyTestdataQuarkusEasyScoreCalculator")
                    .hasMessageContaining(
                            "ai.timefold.solver.quarkus.testdata.dummy.DummyTestdataQuarkusShadowVariableEasyScoreCalculator")
                    .hasMessageContaining("that implements EasyScoreCalculator were found"));

    // Multiple classes - ConstraintProvider
    @RegisterExtension
    static final QuarkusUnitTest config4 = new QuarkusUnitTest()
            .overrideConfigKey("quarkus.timefold.solver.environment-mode", "FULL_ASSERT")
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(TestdataQuarkusEntity.class, TestdataQuarkusSolution.class,
                            TestdataQuarkusConstraintProvider.class)
                    .addClasses(TestdataQuarkusShadowVariableConstraintProvider.class))
            .assertException(t -> assertThat(t)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Multiple score classes")
                    .hasMessageContaining(
                            "ai.timefold.solver.quarkus.testdata.normal.constraints.TestdataQuarkusConstraintProvider")
                    .hasMessageContaining(
                            "ai.timefold.solver.quarkus.testdata.shadowvariable.constraints.TestdataQuarkusShadowVariableConstraintProvider")
                    .hasMessageContaining("that implements ConstraintProvider were found"));
    // Multiple classes - ConstraintProvider with XML
    @RegisterExtension
    static final QuarkusUnitTest config5 = new QuarkusUnitTest()
            .overrideConfigKey("quarkus.timefold.solver.environment-mode", "FULL_ASSERT")
            .overrideConfigKey("quarkus.timefold.solver.solver-config-xml",
                    "ai/timefold/solver/quarkus/customSolverConfigWithoutScore.xml")
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(TestdataQuarkusEntity.class, TestdataQuarkusSolution.class)
                    .addClasses(TestdataQuarkusConstraintProvider.class, TestdataQuarkusShadowVariableConstraintProvider.class)
                    .addAsResource("ai/timefold/solver/quarkus/customSolverConfigWithoutScore.xml"))
            .assertException(t -> assertThat(t)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Multiple score classes")
                    .hasMessageContaining(
                            "ai.timefold.solver.quarkus.testdata.normal.constraints.TestdataQuarkusConstraintProvider")
                    .hasMessageContaining(
                            "ai.timefold.solver.quarkus.testdata.shadowvariable.constraints.TestdataQuarkusShadowVariableConstraintProvider")
                    .hasMessageContaining("that implements ConstraintProvider were found"));

    // Multiple classes - IncrementalScoreCalculator
    @RegisterExtension
    static final QuarkusUnitTest config6 = new QuarkusUnitTest()
            .overrideConfigKey("quarkus.timefold.solver.environment-mode", "FULL_ASSERT")
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(TestdataQuarkusEntity.class, TestdataQuarkusSolution.class,
                            DummyTestdataQuarkusIncrementalScoreCalculator.class)
                    .addClasses(DummyTestdataQuarkusShadowVariableIncrementalScoreCalculator.class))
            .assertException(t -> assertThat(t)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Multiple score classes")
                    .hasMessageContaining(
                            "ai.timefold.solver.quarkus.testdata.dummy.DummyTestdataQuarkusIncrementalScoreCalculator")
                    .hasMessageContaining(
                            "ai.timefold.solver.quarkus.testdata.dummy.DummyTestdataQuarkusShadowVariableIncrementalScoreCalculator")
                    .hasMessageContaining("that implements IncrementalScoreCalculator were found"));
    // Multiple classes - IncrementalScoreCalculator with XML
    @RegisterExtension
    static final QuarkusUnitTest config7 = new QuarkusUnitTest()
            .overrideConfigKey("quarkus.timefold.solver.environment-mode", "FULL_ASSERT")
            .overrideConfigKey("quarkus.timefold.solver.solver-config-xml",
                    "ai/timefold/solver/quarkus/customSolverConfigWithoutScore.xml")
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(TestdataQuarkusEntity.class, TestdataQuarkusSolution.class,
                            TestdataQuarkusConstraintProvider.class)
                    .addClasses(DummyTestdataQuarkusIncrementalScoreCalculator.class,
                            DummyTestdataQuarkusShadowVariableIncrementalScoreCalculator.class)
                    .addAsResource("ai/timefold/solver/quarkus/customSolverConfigWithoutScore.xml"))
            .assertException(t -> assertThat(t)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Multiple score classes")
                    .hasMessageContaining(
                            "ai.timefold.solver.quarkus.testdata.dummy.DummyTestdataQuarkusIncrementalScoreCalculator")
                    .hasMessageContaining(
                            "ai.timefold.solver.quarkus.testdata.dummy.DummyTestdataQuarkusShadowVariableIncrementalScoreCalculator")
                    .hasMessageContaining("that implements IncrementalScoreCalculator were found"));

    @Test
    void test() {
        fail("Should not call this method.");
    }
}
