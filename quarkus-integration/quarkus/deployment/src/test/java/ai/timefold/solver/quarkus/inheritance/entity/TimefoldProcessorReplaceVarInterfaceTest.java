package ai.timefold.solver.quarkus.inheritance.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import ai.timefold.solver.core.impl.testdata.constraints.DummyConstraintProvider;
import ai.timefold.solver.core.impl.testdata.domain.inheritance.entity.single.baseannotated.interfaces.replacevar.TestdataReplaceVarInterfaceChildEntity;
import ai.timefold.solver.core.impl.testdata.domain.inheritance.entity.single.baseannotated.interfaces.replacevar.TestdataReplaceVarInterfaceSolution;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

class TimefoldProcessorReplaceVarInterfaceTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(DummyConstraintProvider.class,
                            TestdataReplaceVarInterfaceSolution.class, TestdataReplaceVarInterfaceChildEntity.class))
            .assertException(exception -> {
                assertEquals(IllegalStateException.class, exception.getClass());
                assertTrue(
                        exception.getMessage().contains(
                                "redefines the genuine variables ([value]), which is not permitted."));
            });

    /**
     * This test validates the behavior of the solver
     * when the annotated child class replaces an existing variable
     * and the base entity is an interface.
     */
    @Test
    void testBothClassesAnnotatedReplaceVariableBaseIsInterface() {
        fail("The build should fail");
    }
}
