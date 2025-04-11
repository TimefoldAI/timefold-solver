package ai.timefold.solver.quarkus.inheritance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import ai.timefold.solver.core.impl.testdata.domain.inheritance.multipleinheritance.baseannotated.childnotannotated.TestMultipleConstraintProvider;
import ai.timefold.solver.core.impl.testdata.domain.inheritance.multipleinheritance.baseannotated.childnotannotated.TestdataBaseEntity;
import ai.timefold.solver.core.impl.testdata.domain.inheritance.multipleinheritance.baseannotated.childnotannotated.TestdataChildEntity;
import ai.timefold.solver.core.impl.testdata.domain.inheritance.multipleinheritance.baseannotated.childnotannotated.TestdataMultipleChildEntity;
import ai.timefold.solver.core.impl.testdata.domain.inheritance.multipleinheritance.baseannotated.childnotannotated.TestdataMultipleSolution;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

class TimefoldProcessorMultipleInheritanceOnlyBaseClassAnnotatedTest {

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
     * when only the base class is annotated with {@code @PlanningEntity} and multiple inheritance is used.
     */
    @Test
    void testMultipleOnlyBaseClassAnnotated() {
        fail("The build should fail");
    }
}
