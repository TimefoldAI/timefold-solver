package ai.timefold.solver.quarkus.inheritance;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import jakarta.inject.Inject;

import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.api.solver.SolverFactory;
import ai.timefold.solver.core.impl.testdata.domain.inheritance.singlevar.baseannotated.childannotated.TestConstraintProvider;
import ai.timefold.solver.core.impl.testdata.domain.inheritance.singlevar.baseannotated.childannotated.TestdataBaseEntity;
import ai.timefold.solver.core.impl.testdata.domain.inheritance.singlevar.baseannotated.childannotated.TestdataChildEntity;
import ai.timefold.solver.core.impl.testdata.domain.inheritance.singlevar.baseannotated.childannotated.TestdataSolution;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

class TimefoldProcessorInheritanceBothClassesAnnotatedTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .overrideConfigKey("quarkus.timefold.solver.termination.best-score-limit", "0")
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(TestConstraintProvider.class, TestdataSolution.class, TestdataChildEntity.class,
                            TestdataBaseEntity.class));
    @Inject
    SolverFactory<TestdataSolution> solverFactory;

    /**
     * This test validates the behavior of the solver
     * when both child and parent classes are annotated with {@code @PlanningEntity}.
     */
    @Test
    void testBothClassesAnnotated() {
        TestdataSolution problem = TestdataSolution.generateSolution(3, 2, false);
        TestdataSolution solution = solverFactory.buildSolver().solve(problem);
        assertNotNull(solution);
        assertThat(solution.getScore()).isEqualTo(SimpleScore.of(2));
    }
}
