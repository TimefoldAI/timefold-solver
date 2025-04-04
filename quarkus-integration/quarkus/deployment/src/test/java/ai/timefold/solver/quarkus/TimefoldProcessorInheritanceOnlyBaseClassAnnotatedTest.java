package ai.timefold.solver.quarkus;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import jakarta.inject.Inject;

import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.api.solver.SolverFactory;
import ai.timefold.solver.core.impl.testdata.domain.superclass.singlevar.baseannotated.childnotannotated.TestConstraintProvider;
import ai.timefold.solver.core.impl.testdata.domain.superclass.singlevar.baseannotated.childnotannotated.TestdataBaseEntity;
import ai.timefold.solver.core.impl.testdata.domain.superclass.singlevar.baseannotated.childnotannotated.TestdataChildEntity;
import ai.timefold.solver.core.impl.testdata.domain.superclass.singlevar.baseannotated.childnotannotated.TestdataSolution;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

class TimefoldProcessorInheritanceOnlyBaseClassAnnotatedTest {

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
     * when only the parent class is annotated with {@code @PlanningEntity}.
     */
    @Test
    void testOnlyBaseClassAnnotated() {
        TestdataSolution problem = TestdataSolution.generateSolution(1, 2);
        TestdataSolution solution = solverFactory.buildSolver().solve(problem);
        assertNotNull(solution);
        assertThat(solution.getScore()).isEqualTo(SimpleScore.of(2));
    }
}
