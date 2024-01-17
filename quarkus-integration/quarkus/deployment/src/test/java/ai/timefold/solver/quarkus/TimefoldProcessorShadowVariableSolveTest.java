package ai.timefold.solver.quarkus;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import jakarta.inject.Inject;

import ai.timefold.solver.core.api.score.ScoreManager;
import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.api.solver.SolutionManager;
import ai.timefold.solver.core.api.solver.SolverFactory;
import ai.timefold.solver.core.api.solver.SolverJob;
import ai.timefold.solver.core.api.solver.SolverManager;
import ai.timefold.solver.core.impl.solver.DefaultSolutionManager;
import ai.timefold.solver.core.impl.solver.DefaultSolverFactory;
import ai.timefold.solver.core.impl.solver.DefaultSolverManager;
import ai.timefold.solver.quarkus.testdata.shadowvariable.constraints.TestdataQuarkusShadowVariableConstraintProvider;
import ai.timefold.solver.quarkus.testdata.shadowvariable.domain.TestdataQuarkusShadowVariableEntity;
import ai.timefold.solver.quarkus.testdata.shadowvariable.domain.TestdataQuarkusShadowVariableListener;
import ai.timefold.solver.quarkus.testdata.shadowvariable.domain.TestdataQuarkusShadowVariableSolution;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

class TimefoldProcessorShadowVariableSolveTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .overrideConfigKey("quarkus.timefold.solver.termination.best-score-limit", "0")
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(TestdataQuarkusShadowVariableEntity.class,
                            TestdataQuarkusShadowVariableSolution.class,
                            TestdataQuarkusShadowVariableConstraintProvider.class,
                            TestdataQuarkusShadowVariableListener.class));

    @Inject
    SolverFactory<TestdataQuarkusShadowVariableSolution> solverFactory;
    @Inject
    SolverManager<TestdataQuarkusShadowVariableSolution, Long> solverManager;
    @Inject
    ScoreManager<TestdataQuarkusShadowVariableSolution, SimpleScore> scoreManager;
    @Inject
    SolutionManager<TestdataQuarkusShadowVariableSolution, SimpleScore> solutionManager;

    @Test
    void singletonSolverFactory() {
        assertNotNull(solverFactory);
        assertSame(((DefaultSolverFactory<TestdataQuarkusShadowVariableSolution>) solverFactory).getScoreDirectorFactory(),
                ((DefaultSolutionManager<TestdataQuarkusShadowVariableSolution, SimpleScore>) solutionManager)
                        .getScoreDirectorFactory());
        assertNotNull(solverManager);
        // There is only one SolverFactory instance
        assertSame(solverFactory,
                ((DefaultSolverManager<TestdataQuarkusShadowVariableSolution, Long>) solverManager).getSolverFactory());
        assertNotNull(solutionManager);
    }

    @Test
    void solve() throws ExecutionException, InterruptedException {
        TestdataQuarkusShadowVariableSolution problem = new TestdataQuarkusShadowVariableSolution();
        problem.setValueList(IntStream.range(1, 3)
                .mapToObj(i -> "v" + i)
                .collect(Collectors.toList()));
        problem.setEntityList(IntStream.range(1, 3)
                .mapToObj(i -> new TestdataQuarkusShadowVariableEntity())
                .collect(Collectors.toList()));
        SolverJob<TestdataQuarkusShadowVariableSolution, Long> solverJob = solverManager.solve(1L, problem);
        TestdataQuarkusShadowVariableSolution solution = solverJob.getFinalBestSolution();
        assertNotNull(solution);
        assertTrue(solution.getScore().score() >= 0);
    }

}
