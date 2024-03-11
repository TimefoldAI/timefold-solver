package ai.timefold.solver.core.impl.solver.termination;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

import ai.timefold.solver.core.impl.score.director.InnerScoreDirector;
import ai.timefold.solver.core.impl.solver.change.DefaultProblemChangeDirector;
import ai.timefold.solver.core.impl.solver.change.ProblemChangeAdapter;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;
import ai.timefold.solver.core.impl.testdata.domain.TestdataSolution;

import org.junit.jupiter.api.Test;

class BasicPlumbingTerminationTest {

    @Test
    void addProblemChangeWithoutDaemon() {
        AtomicInteger count = new AtomicInteger(0);
        BasicPlumbingTermination<TestdataSolution> basicPlumbingTermination = new BasicPlumbingTermination<>(false);
        assertThat(basicPlumbingTermination.waitForRestartSolverDecision()).isFalse();
        ProblemChangeAdapter<TestdataSolution> problemChangeAdapter =
                ProblemChangeAdapter.create((workingSolution, problemChangeDirector) -> count.getAndIncrement());
        basicPlumbingTermination.addProblemChange(problemChangeAdapter);
        assertThat(basicPlumbingTermination.waitForRestartSolverDecision()).isTrue();
        assertThat(count).hasValue(0);

        SolverScope<TestdataSolution> solverScopeMock = mockSolverScope();
        basicPlumbingTermination.startProblemFactChangesProcessing().removeIf(changeAdapter -> {
            changeAdapter.doProblemChange(solverScopeMock);
            return true;
        });
        assertThat(basicPlumbingTermination.waitForRestartSolverDecision()).isFalse();
        assertThat(count).hasValue(1);
    }

    @Test
    void addProblemChangesWithoutDaemon() {
        AtomicInteger count = new AtomicInteger(0);
        BasicPlumbingTermination<TestdataSolution> basicPlumbingTermination = new BasicPlumbingTermination<>(false);
        assertThat(basicPlumbingTermination.waitForRestartSolverDecision()).isFalse();
        basicPlumbingTermination.addProblemChanges(Arrays.asList(
                ProblemChangeAdapter.create((workingSolution, problemChangeDirector) -> count.getAndIncrement()),
                ProblemChangeAdapter.create((workingSolution, problemChangeDirector) -> count.getAndAdd(20))));
        assertThat(basicPlumbingTermination.waitForRestartSolverDecision()).isTrue();
        assertThat(count).hasValue(0);
        SolverScope<TestdataSolution> solverScopeMock = mockSolverScope();
        basicPlumbingTermination.startProblemFactChangesProcessing().removeIf(problemChangeAdapter -> {
            problemChangeAdapter.doProblemChange(solverScopeMock);
            return true;
        });
        assertThat(basicPlumbingTermination.waitForRestartSolverDecision()).isFalse();
        assertThat(count).hasValue(21);
    }

    private SolverScope<TestdataSolution> mockSolverScope() {
        SolverScope<TestdataSolution> solverScope = new SolverScope<>();
        InnerScoreDirector<TestdataSolution, ?> scoreDirectorMock = mock(InnerScoreDirector.class);
        solverScope.setScoreDirector(scoreDirectorMock);
        solverScope.setProblemChangeDirector(new DefaultProblemChangeDirector<>(scoreDirectorMock));
        return solverScope;
    }
}
