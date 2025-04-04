package ai.timefold.solver.quarkus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import jakarta.inject.Inject;

import ai.timefold.solver.core.api.solver.SolverFactory;
import ai.timefold.solver.core.impl.testdata.constraints.DummyConstraintProvider;
import ai.timefold.solver.core.impl.testdata.domain.superclass.singlevar.baseannotated.childannotatedreplacevar.TestdataChildEntity;
import ai.timefold.solver.core.impl.testdata.domain.superclass.singlevar.baseannotated.childannotatedreplacevar.TestdataSolution;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

class TimefoldProcessorInheritanceBothClassesAnnotatedReplaceVarTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .overrideConfigKey("quarkus.timefold.solver.termination.best-score-limit", "0")
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(DummyConstraintProvider.class, TestdataSolution.class, TestdataChildEntity.class))
            .assertException(exception -> assertEquals(IllegalStateException.class, exception.getClass()));

    @Inject
    SolverFactory<TestdataSolution> solverFactory;

    /**
     * This test validates the behavior of the solver
     * when the annotated child class replaces an existing variable.
     */
    @Test
    void testBothClassesAnnotatedReplaceVariable() {
        fail("The build should fail");
    }
}
