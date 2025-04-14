package ai.timefold.solver.quarkus.inheritance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import ai.timefold.solver.core.impl.testdata.domain.inheritance.multiple.baseannotated.interfaces.childtoo.TestMultipleBothAnnotatedInterfaceConstraintProvider;
import ai.timefold.solver.core.impl.testdata.domain.inheritance.multiple.baseannotated.interfaces.childtoo.TestdataMultipleBothAnnotatedInterfaceChildEntity;
import ai.timefold.solver.core.impl.testdata.domain.inheritance.multiple.baseannotated.interfaces.childtoo.TestdataMultipleBothAnnotatedInterfaceSolution;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

class MultipleBothAnnotatedInterfaceTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .overrideConfigKey("quarkus.timefold.solver.termination.best-score-limit", "0")
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(TestMultipleBothAnnotatedInterfaceConstraintProvider.class, TestdataMultipleBothAnnotatedInterfaceSolution.class,
                            TestdataMultipleBothAnnotatedInterfaceChildEntity.class))
            .assertException(exception -> {
                assertEquals(IllegalStateException.class, exception.getClass());
                assertTrue(
                        exception.getMessage().contains(
                                "Remove either the entity classes or entity interfaces from the inheritance chain to create a single-level inheritance structure"));
            });

    /**
     * This test validates the behavior of the solver
     * when both classes are annotated with {@code @PlanningEntity}, multiple inheritance is used,
     * and the base entity is an interface.
     */
    @Test
    void testMultipleBothClassesAnnotatedBaseIsInterface() {
        fail("The build should fail");
    }
}
