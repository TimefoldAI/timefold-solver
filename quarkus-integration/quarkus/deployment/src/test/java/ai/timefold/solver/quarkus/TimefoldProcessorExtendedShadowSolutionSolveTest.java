package ai.timefold.solver.quarkus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;

import java.util.concurrent.ExecutionException;

import jakarta.inject.Inject;

import ai.timefold.solver.core.api.score.ScoreManager;
import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.api.solver.SolutionManager;
import ai.timefold.solver.core.api.solver.SolverFactory;
import ai.timefold.solver.core.api.solver.SolverJob;
import ai.timefold.solver.core.api.solver.SolverManager;
import ai.timefold.solver.core.impl.testdata.domain.TestdataEntity;
import ai.timefold.solver.core.impl.testdata.domain.TestdataObject;
import ai.timefold.solver.core.impl.testdata.domain.TestdataValue;
import ai.timefold.solver.core.impl.testdata.domain.extendedshadow.TestdataExtendedShadowEntity;
import ai.timefold.solver.core.impl.testdata.domain.extendedshadow.TestdataExtendedShadowExtendedShadowEntity;
import ai.timefold.solver.core.impl.testdata.domain.extendedshadow.TestdataExtendedShadowShadowEntity;
import ai.timefold.solver.core.impl.testdata.domain.extendedshadow.TestdataExtendedShadowSolution;
import ai.timefold.solver.quarkus.testdata.extended.TestdataExtendedShadowSolutionConstraintProvider;

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
                            TestdataEntity.class,
                            TestdataObject.class,
                            TestdataValue.class,
                            TestdataExtendedShadowSolutionConstraintProvider.class));

    @Inject
    SolverFactory<TestdataExtendedShadowSolution> solverFactory;
    @Inject
    SolverManager<TestdataExtendedShadowSolution, Long> solverManager;
    @Inject
    ScoreManager<TestdataExtendedShadowSolution, SimpleScore> scoreManager;
    @Inject
    SolutionManager<TestdataExtendedShadowSolution, SimpleScore> solutionManager;

    @Test
    void solve() throws ExecutionException, InterruptedException {
        TestdataExtendedShadowShadowEntity shadowEntity =
                new TestdataExtendedShadowExtendedShadowEntity();
        TestdataExtendedShadowSolution problem = new TestdataExtendedShadowSolution(shadowEntity);
        SolverJob<TestdataExtendedShadowSolution, Long> solverJob = solverManager.solve(1L, problem);
        TestdataExtendedShadowSolution solution = solverJob.getFinalBestSolution();
        assertNotNull(solution);
        assertNotSame(solution, problem);
        assertEquals(0, solution.score.score());
        assertNotSame(solution.shadowEntityList.get(0), problem.shadowEntityList.get(0));
    }

}
