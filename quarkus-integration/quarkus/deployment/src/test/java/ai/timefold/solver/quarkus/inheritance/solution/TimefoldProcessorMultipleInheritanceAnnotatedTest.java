package ai.timefold.solver.quarkus.inheritance.solution;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import ai.timefold.solver.core.impl.testdata.domain.inheritance.solution.baseannotated.multiple.TestdataMultipleInheritanceBaseSolution;
import ai.timefold.solver.core.impl.testdata.domain.inheritance.solution.baseannotated.multiple.TestdataMultipleInheritanceChildSolution;
import ai.timefold.solver.core.impl.testdata.domain.inheritance.solution.baseannotated.multiple.TestdataMultipleInheritanceEntity;
import ai.timefold.solver.core.impl.testdata.domain.inheritance.solution.baseannotated.multiple.TestdataMultipleInheritanceExtendedSolution;
import ai.timefold.solver.quarkus.testdata.superclass.constraints.DummyConstraintProvider;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

class TimefoldProcessorMultipleInheritanceAnnotatedTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .overrideConfigKey("quarkus.timefold.solver.termination.best-score-limit", "0")
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(DummyConstraintProvider.class, TestdataMultipleInheritanceExtendedSolution.class,
                            TestdataMultipleInheritanceChildSolution.class, TestdataMultipleInheritanceBaseSolution.class,
                            TestdataMultipleInheritanceEntity.class))
            .assertException(exception -> {
                assertEquals(IllegalStateException.class, exception.getClass());
                assertTrue(exception.getMessage().contains("Multiple classes"));
                assertTrue(exception.getMessage().contains("found in the classpath with a @PlanningSolution annotation."));
            });

    /**
     * This test validates the behavior of the solver
     * when multiple inheritance is applied.
     */
    @Test
    void testMultipleInheritance() {
        fail("The build should fail");
    }
}
