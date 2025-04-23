package ai.timefold.solver.quarkus.inheritance.solution;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import ai.timefold.solver.core.impl.testdata.domain.inheritance.solution.baseanot.TestdataOnlyAnnotatedBaseEntity;
import ai.timefold.solver.core.impl.testdata.domain.inheritance.solution.baseanot.TestdataOnlyChildAnnotatedChildEntity;
import ai.timefold.solver.core.impl.testdata.domain.inheritance.solution.baseanot.TestdataOnlyChildAnnotatedExtendedSolution;
import ai.timefold.solver.core.impl.testdata.domain.inheritance.solution.baseanot.TestdataOnlyChildAnnotatedSolution;
import ai.timefold.solver.quarkus.testdata.superclass.constraints.DummyConstraintProvider;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

class TimefoldProcessorOnlyChildAnnotatedTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .overrideConfigKey("quarkus.timefold.solver.termination.best-score-limit", "0")
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(DummyConstraintProvider.class, TestdataOnlyChildAnnotatedExtendedSolution.class,
                            TestdataOnlyChildAnnotatedSolution.class, TestdataOnlyChildAnnotatedChildEntity.class,
                            TestdataOnlyAnnotatedBaseEntity.class))
            .assertException(exception -> {
                assertEquals(IllegalStateException.class, exception.getClass());
                assertTrue(exception.getMessage()
                        .contains("is not annotated with @PlanningSolution but defines annotated members"));
                assertTrue(exception.getMessage().contains("Maybe annotate"));
                assertTrue(exception.getMessage().contains("with @PlanningSolution"));
                assertTrue(
                        exception.getMessage().contains("Maybe remove the annotated members ([entityList, score, valueList])"));
            });

    /**
     * This test validates the behavior of the solver
     * when only child class is annotated with {@code @PlanningSolution}.
     */
    @Test
    void testOnlyChildClassAnnotated() {
        fail("The build should fail");
    }
}
