package ai.timefold.solver.quarkus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import ai.timefold.solver.core.impl.testdata.constraints.DummyConstraintProvider;
import ai.timefold.solver.core.impl.testdata.domain.superclass.singlevarinterface.basenotannotated.TestdataChildEntity;
import ai.timefold.solver.core.impl.testdata.domain.superclass.singlevarinterface.basenotannotated.TestdataSolution;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

class TimefoldProcessorInheritanceOnlyChildClassAnnotatedBaseInterfaceTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .overrideConfigKey("quarkus.timefold.solver.termination.best-score-limit", "0")
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(DummyConstraintProvider.class, TestdataSolution.class, TestdataChildEntity.class))
            .assertException(exception -> {
                assertEquals(IllegalStateException.class, exception.getClass());
                assertTrue(
                        exception.getMessage().contains(
                                "should have at least 1 getter method or 1 field with a PlanningVariable annotation or a shadow variable annotation."));
            });

    /**
     * This test validates the behavior of the solver
     * when only the child class is annotated with {@code @PlanningEntity}
     * and the base entity is an interface.
     */
    @Test
    void testOnlyChildClassAnnotatedBaseIsInterface() {
        fail("The build should fail");
    }
}
