package ai.timefold.solver.quarkus.inheritance.solution;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import jakarta.inject.Inject;

import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.api.solver.SolverFactory;
import ai.timefold.solver.core.impl.testdata.domain.inheritance.solution.baseannotated.childtoo.TestdataBothAnnotatedBaseEntity;
import ai.timefold.solver.core.impl.testdata.domain.inheritance.solution.baseannotated.childtoo.TestdataBothAnnotatedChildEntity;
import ai.timefold.solver.core.impl.testdata.domain.inheritance.solution.baseannotated.childtoo.TestdataBothAnnotatedConstraintProvider;
import ai.timefold.solver.core.impl.testdata.domain.inheritance.solution.baseannotated.childtoo.TestdataBothAnnotatedExtendedConstraintProvider;
import ai.timefold.solver.core.impl.testdata.domain.inheritance.solution.baseannotated.childtoo.TestdataBothAnnotatedExtendedSolution;
import ai.timefold.solver.core.impl.testdata.domain.inheritance.solution.baseannotated.childtoo.TestdataBothAnnotatedSolution;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

class TimefoldProcessorBothClassesAnnotatedConfiguredXmlTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .overrideConfigKey("quarkus.timefold.solver.termination.best-score-limit", "0")
            .overrideConfigKey("quarkus.timefold.solver-config-xml",
                    "ai/timefold/solver/quarkus/inheritance/bothClassAnnotatedConfig.xml")
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(TestdataBothAnnotatedExtendedConstraintProvider.class,
                            TestdataBothAnnotatedConstraintProvider.class,
                            TestdataBothAnnotatedExtendedSolution.class, TestdataBothAnnotatedSolution.class,
                            TestdataBothAnnotatedChildEntity.class,
                            TestdataBothAnnotatedBaseEntity.class)
                    .addAsResource("ai/timefold/solver/quarkus/inheritance/bothClassAnnotatedConfig.xml"));

    @Inject
    SolverFactory<TestdataBothAnnotatedExtendedSolution> solverFactory;

    /**
     * This test validates the behavior of the solver
     * when both child and parent solution classes are annotated with {@code @PlanningSolution}.
     */
    @Test
    void testBothClassesAnnotated() {
        var problem = TestdataBothAnnotatedExtendedSolution.generateSolution(3, 2);
        var solution = solverFactory.buildSolver().solve(problem);
        assertNotNull(solution);
        assertThat(solution.getScore()).isEqualTo(SimpleScore.of(8));
    }
}
