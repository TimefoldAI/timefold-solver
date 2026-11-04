package ai.timefold.solver.core.impl.solver;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

import ai.timefold.solver.core.api.solver.SolverFactory;
import ai.timefold.solver.core.testdomain.TestdataEntity;
import ai.timefold.solver.core.testdomain.TestdataSolution;
import ai.timefold.solver.core.testutil.PlannerTestUtils;

import org.junit.jupiter.api.Test;

class DefaultSolverJobTest {

    @Test
    @SuppressWarnings("unchecked")
    void terminatedBeforeFutureIsRegistered() throws InterruptedException, ExecutionException {
        var solverConfig = PlannerTestUtils.buildSolverConfig(TestdataSolution.class, TestdataEntity.class);
        var solver = SolverFactory.<TestdataSolution> create(solverConfig).buildSolver();
        var problem = TestdataSolution.generateSolution(2, 2);
        var solverJob = new DefaultSolverJob<TestdataSolution>(mock(DefaultSolverManager.class), solver, 1L,
                id -> problem, null, null, null, null, (id, throwable) -> {
                });

        solverJob.terminateEarly(); // No future is registered yet, so nothing is cancelled.
        var future = new FutureTask<>(solverJob);
        future.run();
        solverJob.setFinalBestSolutionFuture(future);

        assertThatThrownBy(solverJob::getResult).isInstanceOf(CancellationException.class);
        assertThat(solverJob.getFinalBestSolution()).isSameAs(problem);
    }

}
