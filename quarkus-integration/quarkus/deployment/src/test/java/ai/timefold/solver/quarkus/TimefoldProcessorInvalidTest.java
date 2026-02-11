package ai.timefold.solver.quarkus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import jakarta.inject.Inject;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.InverseRelationShadowVariable;
import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.api.solver.SolutionManager;
import ai.timefold.solver.core.api.solver.SolverFactory;
import ai.timefold.solver.core.api.solver.SolverManager;
import ai.timefold.solver.quarkus.testdomain.invalid.inverserelation.TestdataInvalidInverseRelationEntity;
import ai.timefold.solver.quarkus.testdomain.invalid.inverserelation.TestdataInvalidInverseRelationSolution;
import ai.timefold.solver.quarkus.testdomain.invalid.inverserelation.TestdataInvalidInverseRelationValue;
import ai.timefold.solver.quarkus.testdomain.invalid.inverserelation.TestdataInvalidQuarkusConstraintProvider;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

class TimefoldProcessorInvalidTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(TestdataInvalidInverseRelationSolution.class,
                            TestdataInvalidInverseRelationEntity.class,
                            TestdataInvalidInverseRelationValue.class,
                            TestdataInvalidQuarkusConstraintProvider.class))
            .assertException(exception -> {
                assertEquals(IllegalStateException.class, exception.getClass());
                assertEquals("""
                        The field (entityList) with a @%s annotation is \
                        in a class (%s) \
                        that does not have a @%s annotation.
                        Maybe add a @%s annotation on the class (%s)."""
                        .formatted(InverseRelationShadowVariable.class.getSimpleName(),
                                TestdataInvalidInverseRelationValue.class.getName(),
                                PlanningEntity.class.getSimpleName(),
                                PlanningEntity.class.getSimpleName(),
                                TestdataInvalidInverseRelationValue.class.getName()),
                        exception.getMessage());
            });

    @Inject
    SolverFactory<TestdataInvalidInverseRelationSolution> solverFactory;
    @Inject
    SolverManager<TestdataInvalidInverseRelationSolution, Long> solverManager;
    @Inject
    SolutionManager<TestdataInvalidInverseRelationSolution, SimpleScore> solutionManager;

    @Test
    void solve() {
        fail("Build should fail");
    }

}
