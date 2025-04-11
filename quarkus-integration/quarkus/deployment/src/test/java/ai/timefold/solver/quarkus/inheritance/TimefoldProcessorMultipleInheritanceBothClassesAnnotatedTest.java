package ai.timefold.solver.quarkus.inheritance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import ai.timefold.solver.core.impl.testdata.domain.inheritance.multipleinheritance.baseannotated.childannotated.TestMultipleConstraintProvider;
import ai.timefold.solver.core.impl.testdata.domain.inheritance.multipleinheritance.baseannotated.childannotated.TestdataMultipleChildEntity;
import ai.timefold.solver.core.impl.testdata.domain.inheritance.multipleinheritance.baseannotated.childannotated.TestdataMultipleSolution;
import ai.timefold.solver.core.impl.testdata.domain.inheritance.singlevar.baseannotated.childannotated.TestdataBaseEntity;
import ai.timefold.solver.core.impl.testdata.domain.inheritance.singlevar.baseannotated.childannotated.TestdataChildEntity;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

class TimefoldProcessorMultipleInheritanceBothClassesAnnotatedTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .overrideConfigKey("quarkus.timefold.solver.termination.best-score-limit", "0")
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(TestMultipleConstraintProvider.class, TestdataMultipleSolution.class,
                            TestdataMultipleChildEntity.class, TestdataChildEntity.class, TestdataBaseEntity.class))
            .assertException(exception -> {
                assertEquals(IllegalStateException.class, exception.getClass());
                assertTrue(
                        exception.getMessage().contains(
                                "The multiple inheritance is not allowed."));
            });

    /**
     * This test validates the behavior of the solver
     * when both classes are annotated with {@code @PlanningEntity} and multiple inheritance is used.
     */
    @Test
    void testMultipleBothClassesAnnotated() {
        fail("The build should fail");
    }
}
