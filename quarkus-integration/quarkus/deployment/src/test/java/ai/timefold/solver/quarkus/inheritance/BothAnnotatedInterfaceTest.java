package ai.timefold.solver.quarkus.inheritance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import jakarta.inject.Inject;

import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.api.solver.SolverFactory;
import ai.timefold.solver.core.impl.testdata.domain.inheritance.single.baseannotated.interfaces.childtoo.TestBothAnnotatedInterfaceConstraintProvider;
import ai.timefold.solver.core.impl.testdata.domain.inheritance.single.baseannotated.interfaces.childtoo.TestdataBaseEntity;
import ai.timefold.solver.core.impl.testdata.domain.inheritance.single.baseannotated.interfaces.childtoo.TestdataBothAnnotatedInterfaceChildEntity;
import ai.timefold.solver.core.impl.testdata.domain.inheritance.single.baseannotated.interfaces.childtoo.TestdataBothAnnotatedInterfaceSolution;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

class BothAnnotatedInterfaceTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .overrideConfigKey("quarkus.timefold.solver.termination.best-score-limit", "0")
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(TestBothAnnotatedInterfaceConstraintProvider.class,
                            TestdataBothAnnotatedInterfaceSolution.class, TestdataBothAnnotatedInterfaceChildEntity.class,
                            TestdataBaseEntity.class));
    @Inject
    SolverFactory<TestdataBothAnnotatedInterfaceSolution> solverFactory;

    /**
     * This test validates the behavior of the solver
     * when both child and parent classes are annotated with {@code @PlanningEntity}
     * and the base entity is an interface.
     */
    @Test
    void testBothClassesAnnotatedBaseIsInterface() {
        var problem = TestdataBothAnnotatedInterfaceSolution.generateSolution(3, 2, false);
        var solution = solverFactory.buildSolver().solve(problem);
        assertNotNull(solution);
        assertThat(solution.getScore()).isEqualTo(SimpleScore.of(2));
    }
}
