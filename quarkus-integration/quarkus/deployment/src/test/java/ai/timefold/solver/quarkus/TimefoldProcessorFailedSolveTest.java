package ai.timefold.solver.quarkus;

import static ai.timefold.solver.quarkus.testdomain.failing.TestdataQuarkusFailingConstraintProvider.FAILING_VALUE;
import static ai.timefold.solver.quarkus.testdomain.failing.TestdataQuarkusFailingConstraintProvider.FAILURE_MESSAGE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.stream.IntStream;

import jakarta.inject.Inject;

import ai.timefold.solver.core.api.solver.SolverManager;
import ai.timefold.solver.core.api.solver.SolverStatus;
import ai.timefold.solver.quarkus.testdomain.failing.TestdataQuarkusFailingConstraintProvider;
import ai.timefold.solver.quarkus.testdomain.normal.TestdataQuarkusEntity;
import ai.timefold.solver.quarkus.testdomain.normal.TestdataQuarkusSolution;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

/**
 * A Quarkus application keeps one {@link SolverManager} for its whole lifetime,
 * so whatever a failed solve leaves behind accumulates rather than being collected along with a request.
 * These tests submit failing jobs and check that the application is no worse off afterwards.
 * <p>
 * The failure is raised during score calculation, which puts it inside {@code DefaultSolver.solve}'s try block —
 * the window whose {@code catch} calls {@code solvingError}, which is what closes the score director.
 * {@code DefaultSolver} otherwise closes it only in {@code outerSolvingEnded}, which a failed solve never reaches.
 * The closing itself cannot be observed from here, as nothing in the Quarkus layer hands out the score director:
 * {@code DefaultSolverTest.solvingErrorClosesScoreDirectorWhenPhaseFails} and
 * {@code DefaultSolverTest.solvingErrorClosesScoreDirectorWhenSolvingStartedFails} pin that, and both fail if
 * {@code AbstractSolver.solvingError} stops calling {@code solverContextManager.solvingError(..)}.
 */
class TimefoldProcessorFailedSolveTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .overrideConfigKey("quarkus.timefold.solver.termination.best-score-limit", "0")
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(TestdataQuarkusEntity.class,
                            TestdataQuarkusSolution.class,
                            TestdataQuarkusFailingConstraintProvider.class));

    @Inject
    SolverManager<TestdataQuarkusSolution> solverManager;

    /**
     * @param failing true to put the value that makes score calculation throw into the value range
     */
    private static TestdataQuarkusSolution buildProblem(boolean failing) {
        var problem = new TestdataQuarkusSolution();
        problem.setValueList(failing ? List.of(FAILING_VALUE) : List.of("v1", "v2"));
        problem.setEntityList(IntStream.range(0, 2)
                .mapToObj(i -> new TestdataQuarkusEntity())
                .toList());
        return problem;
    }

    @Test
    void twoFailedSolvesLeaveTheSolverManagerUsable() throws Exception {
        for (var problemId : List.of(1L, 2L)) {
            var solverJob = solverManager.solve(problemId, buildProblem(true));

            assertThatThrownBy(solverJob::getFinalBestSolution)
                    .rootCause()
                    // The caller gets the real cause. A cleanup step failing on its way out would surface
                    // here instead, and the actual problem would be lost.
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining(FAILURE_MESSAGE);

            // The job is unregistered even though it failed, or the manager retains it for the life of the app.
            assertThat(solverManager.getSolverStatus(problemId)).isEqualTo(SolverStatus.NOT_SOLVING);
        }

        // The manager is a singleton that outlives its jobs, so it has to still work after both failures.
        var solution = solverManager.solve(3L, buildProblem(false)).getFinalBestSolution();
        assertThat(solution).isNotNull();
        assertThat(solution.getScore()).isNotNull();
    }
}
