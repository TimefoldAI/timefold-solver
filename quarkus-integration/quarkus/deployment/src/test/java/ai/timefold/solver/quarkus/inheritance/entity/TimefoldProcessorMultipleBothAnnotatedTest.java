package ai.timefold.solver.quarkus.inheritance.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import ai.timefold.solver.core.impl.testdata.domain.TestdataEntity;
import ai.timefold.solver.core.impl.testdata.domain.inheritance.entity.multiple.baseannotated.classes.childtoo.TestMultipleBothAnnotatedConstraintProvider;
import ai.timefold.solver.core.impl.testdata.domain.inheritance.entity.multiple.baseannotated.classes.childtoo.TestdataMultipleBothAnnotatedChildEntity;
import ai.timefold.solver.core.impl.testdata.domain.inheritance.entity.multiple.baseannotated.classes.childtoo.TestdataMultipleBothAnnotatedSolution;
import ai.timefold.solver.core.impl.testdata.domain.inheritance.entity.single.baseannotated.classes.childtoo.TestdataBothAnnotatedChildEntity;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

class TimefoldProcessorMultipleBothAnnotatedTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(TestMultipleBothAnnotatedConstraintProvider.class, TestdataMultipleBothAnnotatedSolution.class,
                            TestdataMultipleBothAnnotatedChildEntity.class, TestdataBothAnnotatedChildEntity.class,
                            TestdataEntity.class))
            .assertException(exception -> {
                assertEquals(IllegalStateException.class, exception.getClass());
                assertTrue(
                        exception.getMessage().contains(
                                "Remove either the entity classes or entity interfaces from the inheritance chain to create a single-level inheritance structure"));
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
