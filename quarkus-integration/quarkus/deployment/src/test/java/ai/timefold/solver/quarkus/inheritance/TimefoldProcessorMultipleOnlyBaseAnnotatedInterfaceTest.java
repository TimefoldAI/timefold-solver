package ai.timefold.solver.quarkus.inheritance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import ai.timefold.solver.core.impl.testdata.domain.inheritance.multiple.baseannotated.interfaces.childnot.TestMultipleChildNotAnnotatedInterfaceConstraintProvider;
import ai.timefold.solver.core.impl.testdata.domain.inheritance.multiple.baseannotated.interfaces.childnot.TestdataMultipleChildNotAnnotatedInterfaceBaseEntity;
import ai.timefold.solver.core.impl.testdata.domain.inheritance.multiple.baseannotated.interfaces.childnot.TestdataMultipleChildNotAnnotatedInterfaceChildEntity;
import ai.timefold.solver.core.impl.testdata.domain.inheritance.multiple.baseannotated.interfaces.childnot.TestdataMultipleChildNotAnnotatedInterfaceSecondEntity;
import ai.timefold.solver.core.impl.testdata.domain.inheritance.multiple.baseannotated.interfaces.childnot.TestdataMultipleChildNotAnnotatedInterfaceSolution;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

class TimefoldProcessorMultipleOnlyBaseAnnotatedInterfaceTest {
    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(TestMultipleChildNotAnnotatedInterfaceConstraintProvider.class,
                            TestdataMultipleChildNotAnnotatedInterfaceSolution.class,
                            TestdataMultipleChildNotAnnotatedInterfaceChildEntity.class,
                            TestdataMultipleChildNotAnnotatedInterfaceSecondEntity.class,
                            TestdataMultipleChildNotAnnotatedInterfaceBaseEntity.class))
            .assertException(exception -> {
                assertEquals(IllegalStateException.class, exception.getClass());
                assertTrue(
                        exception.getMessage().contains(
                                "Remove either the entity classes or entity interfaces from the inheritance chain to create a single-level inheritance structure"));
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
