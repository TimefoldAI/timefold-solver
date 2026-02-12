package ai.timefold.solver.quarkus.inheritance.solution;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import jakarta.inject.Inject;

import ai.timefold.solver.core.api.score.SimpleScore;
import ai.timefold.solver.core.api.solver.SolverFactory;
import ai.timefold.solver.core.testdomain.inheritance.solution.baseannotated.childtooabstract.TestdataBothAnnotatedAbstractBaseEntity;
import ai.timefold.solver.core.testdomain.inheritance.solution.baseannotated.childtooabstract.TestdataBothAnnotatedAbstractChildEntity;
import ai.timefold.solver.core.testdomain.inheritance.solution.baseannotated.childtooabstract.TestdataBothAnnotatedAbstractConstraintProvider;
import ai.timefold.solver.core.testdomain.inheritance.solution.baseannotated.childtooabstract.TestdataBothAnnotatedAbstractExtendedConstraintProvider;
import ai.timefold.solver.core.testdomain.inheritance.solution.baseannotated.childtooabstract.TestdataBothAnnotatedAbstractExtendedSolution;
import ai.timefold.solver.core.testdomain.inheritance.solution.baseannotated.childtooabstract.TestdataBothAnnotatedAbstractSolution;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

class TimefoldProcessorBothClassesAnnotatedAbstractTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .overrideConfigKey("quarkus.timefold.solver.termination.best-score-limit", "0")
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(TestdataBothAnnotatedAbstractExtendedConstraintProvider.class,
                            TestdataBothAnnotatedAbstractConstraintProvider.class,
                            TestdataBothAnnotatedAbstractExtendedSolution.class,
                            TestdataBothAnnotatedAbstractSolution.class, TestdataBothAnnotatedAbstractChildEntity.class,
                            TestdataBothAnnotatedAbstractBaseEntity.class));
    @Inject
    SolverFactory<TestdataBothAnnotatedAbstractExtendedSolution> solverFactory;

    /**
     * This test validates the behavior of the solver
     * when both child and parent solution classes are annotated with {@code @PlanningSolution},
     * and the base class is abstract.
     */
    @Test
    void testBothClassesAnnotatedBaseAbstract() {
        var problem = TestdataBothAnnotatedAbstractExtendedSolution.generateSolution(3, 2);
        var solution = solverFactory.buildSolver().solve(problem);
        assertNotNull(solution);
        assertThat(solution.getScore()).isEqualTo(SimpleScore.of(8));
    }
}
