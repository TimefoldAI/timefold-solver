package ai.timefold.solver.quarkus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;

import java.util.concurrent.ExecutionException;

import jakarta.inject.Inject;

import ai.timefold.solver.core.api.solver.SolverManager;
import ai.timefold.solver.core.testdomain.TestdataEntity;
import ai.timefold.solver.core.testdomain.TestdataObject;
import ai.timefold.solver.core.testdomain.TestdataValue;
import ai.timefold.solver.core.testdomain.inheritance.entity.single.baseannotated.classes.shadow.TestdataExtendedShadowEntity;
import ai.timefold.solver.core.testdomain.inheritance.entity.single.baseannotated.classes.shadow.TestdataExtendedShadowExtendedShadowEntity;
import ai.timefold.solver.core.testdomain.inheritance.entity.single.baseannotated.classes.shadow.TestdataExtendedShadowShadowEntity;
import ai.timefold.solver.core.testdomain.inheritance.entity.single.baseannotated.classes.shadow.TestdataExtendedShadowSolution;
import ai.timefold.solver.core.testdomain.inheritance.entity.single.baseannotated.classes.shadow.TestdataExtendedShadowVariable;
import ai.timefold.solver.quarkus.testdomain.inheritance.solution.TestdataExtendedShadowSolutionConstraintProvider;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

class TimefoldProcessorExtendedShadowSolutionSolveTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .overrideConfigKey("quarkus.timefold.solver.termination.best-score-limit", "0")
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(TestdataExtendedShadowSolution.class,
                            TestdataExtendedShadowEntity.class,
                            TestdataExtendedShadowShadowEntity.class,
                            TestdataExtendedShadowExtendedShadowEntity.class,
                            TestdataExtendedShadowVariable.class,
                            TestdataEntity.class,
                            TestdataObject.class,
                            TestdataValue.class,
                            TestdataExtendedShadowSolutionConstraintProvider.class));
    @Inject
    SolverManager<TestdataExtendedShadowSolution, Long> solverManager;

    @Test
    void solve() throws ExecutionException, InterruptedException {
        var shadowEntity = new TestdataExtendedShadowExtendedShadowEntity();
        var problem = new TestdataExtendedShadowSolution(shadowEntity);
        var solverJob = solverManager.solve(1L, problem);
        var solution = solverJob.getFinalBestSolution();
        assertNotNull(solution);
        assertNotSame(solution, problem);
        assertEquals(0, solution.score.score());
        assertNotSame(solution.shadowEntityList.get(0), problem.shadowEntityList.get(0));
    }

}
