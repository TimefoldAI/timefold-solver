package ai.timefold.solver.quarkus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import ai.timefold.solver.core.impl.testdata.domain.superclass.multipleinheritance.baseannotated.mixed.childannotated.TestMultipleConstraintProvider;
import ai.timefold.solver.core.impl.testdata.domain.superclass.multipleinheritance.baseannotated.mixed.childannotated.TestdataMultipleChildEntity;
import ai.timefold.solver.core.impl.testdata.domain.superclass.multipleinheritance.baseannotated.mixed.childannotated.TestdataMultipleSolution;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

class TimefoldProcessorMultipleInheritanceBothClassesMixedPatternAnnotatedTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .overrideConfigKey("quarkus.timefold.solver.termination.best-score-limit", "0")
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(TestMultipleConstraintProvider.class, TestdataMultipleSolution.class,
                            TestdataMultipleChildEntity.class))
            .assertException(exception -> assertEquals(IllegalStateException.class, exception.getClass()));

    /**
     * This test validates the behavior of the solver
     * when multiple inheritance is used, the child is annotated with {@code @PlanningEntity}
     * and it inherits from class and interface.
     */
    @Test
    void testMultipleBothClassesAnnotatedMixedPattern() {
        fail("The build should fail");
    }
}
