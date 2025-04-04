package ai.timefold.solver.quarkus;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import jakarta.inject.Inject;

import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.api.solver.SolverFactory;
import ai.timefold.solver.core.impl.testdata.domain.superclass.multipleinheritanceinterface.baseannotated.childannotated.TestMultipleConstraintProvider;
import ai.timefold.solver.core.impl.testdata.domain.superclass.multipleinheritanceinterface.baseannotated.childannotated.TestdataMultipleChildEntity;
import ai.timefold.solver.core.impl.testdata.domain.superclass.multipleinheritanceinterface.baseannotated.childannotated.TestdataMultipleSolution;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

class TimefoldProcessorMultipleInheritanceBothClassesAnnotatedBaseInterfaceTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .overrideConfigKey("quarkus.timefold.solver.termination.best-score-limit", "0")
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(TestMultipleConstraintProvider.class, TestdataMultipleSolution.class,
                            TestdataMultipleChildEntity.class));
    @Inject
    SolverFactory<TestdataMultipleSolution> solverFactory;

    /**
     * This test validates the behavior of the solver
     * when both classes are annotated with {@code @PlanningEntity}, multiple inheritance is used,
     * and the base entity is an interface.
     */
    @Test
    void testMultipleBothClassesAnnotatedBaseIsInterface() {
        TestdataMultipleSolution problem = TestdataMultipleSolution.generateSolution(1, 2, false);
        TestdataMultipleSolution solution = solverFactory.buildSolver().solve(problem);
        assertNotNull(solution);
        assertThat(solution.getScore()).isEqualTo(SimpleScore.of(4)); // value of 2 for each constraint
    }
}
