package ai.timefold.solver.core.impl.solver.termination;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;

import ai.timefold.solver.core.api.solver.change.ProblemChange;
import ai.timefold.solver.core.impl.score.director.InnerScoreDirector;
import ai.timefold.solver.core.impl.solver.change.DefaultProblemChangeDirector;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;
import ai.timefold.solver.core.testdomain.TestdataSolution;

import org.junit.jupiter.api.Test;

class BasicPlumbingTerminationTest {

    @Test
    void addProblemChangeWithoutDaemon() {
        var count = new AtomicInteger(0);
        var basicPlumbingTermination = new BasicPlumbingTermination<TestdataSolution>(false);
        assertThat(basicPlumbingTermination.waitForRestartSolverDecision()).isFalse();
        ProblemChange<TestdataSolution> problemChangeAdapter =
                (workingSolution, problemChangeDirector) -> count.getAndIncrement();
        basicPlumbingTermination.addProblemChanges(Collections.singletonList(problemChangeAdapter));
        assertThat(basicPlumbingTermination.waitForRestartSolverDecision()).isTrue();
        assertThat(count).hasValue(0);

        var solverScopeMock = mockSolverScope();
        basicPlumbingTermination.startProblemChangesProcessing().removeIf(problemChange -> {
            problemChange.doChange(solverScopeMock.getWorkingSolution(), solverScopeMock.getProblemChangeDirector());
            return true;
        });
        assertThat(basicPlumbingTermination.waitForRestartSolverDecision()).isFalse();
        assertThat(count).hasValue(1);
    }

    @Test
    void addProblemChangesWithoutDaemon() {
        var count = new AtomicInteger(0);
        var basicPlumbingTermination = new BasicPlumbingTermination<TestdataSolution>(false);
        assertThat(basicPlumbingTermination.waitForRestartSolverDecision()).isFalse();
        basicPlumbingTermination.addProblemChanges(Arrays.asList(
                (workingSolution, problemChangeDirector) -> count.getAndIncrement(),
                (workingSolution, problemChangeDirector) -> count.getAndAdd(20)));
        assertThat(basicPlumbingTermination.waitForRestartSolverDecision()).isTrue();
        assertThat(count).hasValue(0);
        var solverScopeMock = mockSolverScope();
        basicPlumbingTermination.startProblemChangesProcessing().removeIf(problemChange -> {
            problemChange.doChange(solverScopeMock.getWorkingSolution(), solverScopeMock.getProblemChangeDirector());
            return true;
        });
        assertThat(basicPlumbingTermination.waitForRestartSolverDecision()).isFalse();
        assertThat(count).hasValue(21);
    }

    private SolverScope<TestdataSolution> mockSolverScope() {
        var solverScope = new SolverScope<TestdataSolution>();
        InnerScoreDirector<TestdataSolution, ?> scoreDirectorMock = mock(InnerScoreDirector.class);
        solverScope.setScoreDirector(scoreDirectorMock);
        solverScope.setProblemChangeDirector(new DefaultProblemChangeDirector<>(scoreDirectorMock));
        return solverScope;
    }
}
