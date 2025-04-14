package ai.timefold.solver.quarkus.inheritance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import ai.timefold.solver.core.impl.testdata.domain.inheritance.multiple.baseannotated.classes.mixed.TestMultipleMixedConstraintProvider;
import ai.timefold.solver.core.impl.testdata.domain.inheritance.multiple.baseannotated.classes.mixed.TestdataMultipleMixedBaseEntity;
import ai.timefold.solver.core.impl.testdata.domain.inheritance.multiple.baseannotated.classes.mixed.TestdataMultipleMixedChildEntity;
import ai.timefold.solver.core.impl.testdata.domain.inheritance.multiple.baseannotated.classes.mixed.TestdataMultipleMixedSolution;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

class TimefoldProcessorMultipleMixedAnnotatedTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(TestMultipleMixedConstraintProvider.class, TestdataMultipleMixedSolution.class,
                            TestdataMultipleMixedChildEntity.class, TestdataMultipleMixedBaseEntity.class))
            .assertException(exception -> {
                assertEquals(IllegalStateException.class, exception.getClass());
                assertTrue(
                        exception.getMessage().contains(
                                "Mixed inheritance is not permitted."));
            });

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
