package ai.timefold.solver.quarkus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import javax.inject.Inject;

import ai.timefold.solver.core.api.score.ScoreManager;
import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.api.solver.SolutionManager;
import ai.timefold.solver.core.api.solver.SolverFactory;
import ai.timefold.solver.core.api.solver.SolverManager;
import ai.timefold.solver.quarkus.testdata.invalid.inverserelation.constraints.TestdataInvalidQuarkusConstraintProvider;
import ai.timefold.solver.quarkus.testdata.invalid.inverserelation.domain.TestdataInvalidInverseRelationEntity;
import ai.timefold.solver.quarkus.testdata.invalid.inverserelation.domain.TestdataInvalidInverseRelationSolution;
import ai.timefold.solver.quarkus.testdata.invalid.inverserelation.domain.TestdataInvalidInverseRelationValue;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

class OptaPlannerProcessorInvalidTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .overrideConfigKey("quarkus.optaplanner.solver.termination.best-score-limit", "0")
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(TestdataInvalidInverseRelationSolution.class,
                            TestdataInvalidInverseRelationEntity.class,
                            TestdataInvalidInverseRelationValue.class,
                            TestdataInvalidQuarkusConstraintProvider.class))
            .assertException(exception -> {
                assertEquals(IllegalStateException.class, exception.getClass());
                assertEquals("The field (entityList) with a @InverseRelationShadowVariable annotation is" +
                        " in a class (org.optaplanner.quarkus.testdata.invalid.inverserelation.domain.TestdataInvalidInverseRelationValue)"
                        +
                        " that does not have a @PlanningEntity annotation.\n" +
                        "Maybe add a @PlanningEntity annotation on the class (org.optaplanner.quarkus.testdata.invalid.inverserelation.domain.TestdataInvalidInverseRelationValue).",
                        exception.getMessage());
            });

    @Inject
    SolverFactory<TestdataInvalidInverseRelationSolution> solverFactory;
    @Inject
    SolverManager<TestdataInvalidInverseRelationSolution, Long> solverManager;
    @Inject
    ScoreManager<TestdataInvalidInverseRelationSolution, SimpleScore> scoreManager;
    @Inject
    SolutionManager<TestdataInvalidInverseRelationSolution, SimpleScore> solutionManager;

    @Test
    void solve() {
        fail("Build should fail");
    }

}
