package ai.timefold.solver.quarkus.inheritance.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import ai.timefold.solver.core.impl.testdata.constraints.DummyConstraintProvider;
import ai.timefold.solver.core.impl.testdata.domain.inheritance.entity.single.baseannotated.classes.replacevar.TestdataReplaceVarChildEntity;
import ai.timefold.solver.core.impl.testdata.domain.inheritance.entity.single.baseannotated.classes.replacevar.TestdataReplaceVarSolution;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

class TimefoldProcessorReplaceVarTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(DummyConstraintProvider.class, TestdataReplaceVarSolution.class,
                            TestdataReplaceVarChildEntity.class))
            .assertException(exception -> {
                assertEquals(IllegalStateException.class, exception.getClass());
                assertTrue(
                        exception.getMessage().contains(
                                "redefines the genuine variables ([value]), which is not permitted."));
            });

    /**
     * This test validates the behavior of the solver
     * when the annotated child class replaces an existing variable.
     */
    @Test
    void testBothClassesAnnotatedReplaceVariable() {
        fail("The build should fail");
    }
}
