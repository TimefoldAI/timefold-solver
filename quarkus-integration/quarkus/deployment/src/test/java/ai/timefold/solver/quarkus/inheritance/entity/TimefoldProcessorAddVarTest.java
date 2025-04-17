package ai.timefold.solver.quarkus.inheritance.entity;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import jakarta.inject.Inject;

import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.api.solver.SolverFactory;
import ai.timefold.solver.core.impl.testdata.domain.inheritance.entity.single.baseannotated.classes.addvar.TestAddVarConstraintProvider;
import ai.timefold.solver.core.impl.testdata.domain.inheritance.entity.single.baseannotated.classes.addvar.TestdataAddVarBaseEntity;
import ai.timefold.solver.core.impl.testdata.domain.inheritance.entity.single.baseannotated.classes.addvar.TestdataAddVarChildEntity;
import ai.timefold.solver.core.impl.testdata.domain.inheritance.entity.single.baseannotated.classes.addvar.TestdataAddVarSolution;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

class TimefoldProcessorAddVarTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .overrideConfigKey("quarkus.timefold.solver.termination.best-score-limit", "0")
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(TestAddVarConstraintProvider.class, TestdataAddVarSolution.class,
                            TestdataAddVarChildEntity.class,
                            TestdataAddVarBaseEntity.class));
    @Inject
    SolverFactory<TestdataAddVarSolution> solverFactory;

    /**
     * This test validates the behavior of the solver
     * when the annotated child class adds new variables.
     */
    @Test
    void testBothClassesAnnotatedAddNewVariable() {
        var problem = TestdataAddVarSolution.generateSolution(3, 2, true);
        var solution = solverFactory.buildSolver().solve(problem);
        assertNotNull(solution);
        assertThat(solution.getScore()).isEqualTo(SimpleScore.of(4)); // value of 2 for each constraint
    }
}
