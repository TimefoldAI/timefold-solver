package ai.timefold.solver.quarkus.inheritance.solution;

import static org.assertj.core.api.Fail.fail;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.timefold.solver.core.impl.testdata.domain.inheritance.solution.baseannotated.childtoo.TestdataBothAnnotatedBaseEntity;
import ai.timefold.solver.core.impl.testdata.domain.inheritance.solution.baseannotated.childtoo.TestdataBothAnnotatedChildEntity;
import ai.timefold.solver.core.impl.testdata.domain.inheritance.solution.baseannotated.childtoo.TestdataBothAnnotatedConstraintProvider;
import ai.timefold.solver.core.impl.testdata.domain.inheritance.solution.baseannotated.childtoo.TestdataBothAnnotatedExtendedConstraintProvider;
import ai.timefold.solver.core.impl.testdata.domain.inheritance.solution.baseannotated.childtoo.TestdataBothAnnotatedExtendedSolution;
import ai.timefold.solver.core.impl.testdata.domain.inheritance.solution.baseannotated.childtoo.TestdataBothAnnotatedSolution;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

class TimefoldProcessorBothClassesAnnotatedTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(TestdataBothAnnotatedExtendedConstraintProvider.class,
                            TestdataBothAnnotatedConstraintProvider.class, TestdataBothAnnotatedExtendedSolution.class,
                            TestdataBothAnnotatedSolution.class, TestdataBothAnnotatedChildEntity.class,
                            TestdataBothAnnotatedBaseEntity.class))
            .assertException(exception -> {
                assertEquals(IllegalStateException.class, exception.getClass());
                assertTrue(exception.getMessage().contains("Multiple classes"));
                assertTrue(exception.getMessage().contains("found in the classpath with a @PlanningSolution annotation."));
            });

    /**
     * This test validates the behavior of the solver
     * when both child and parent solution classes are annotated with {@code @PlanningSolution}.
     */
    @Test
    void testBothClassesAnnotated() {
        fail("The build should fail");
    }
}
