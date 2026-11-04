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

    private static void addNoopProblemChange(BasicPlumbingTermination<TestdataSolution> termination) {
        termination.addProblemChanges(Collections.singletonList((workingSolution, problemChangeDirector) -> {
        }));
    }

    @Test
    void terminateEarlyBeforeSolvingEndedIsEarly() {
        var termination = new BasicPlumbingTermination<TestdataSolution>(false);
        termination.terminateEarly();
        termination.solvingEnded(mock(SolverScope.class));
        assertThat(termination.waitForRestartSolverDecision()).isFalse();
        assertThat(termination.isRunEndedByTerminateEarly()).isTrue();
    }

    @Test
    void terminateEarlyAfterSolvingEndedIsNotEarly() {
        var termination = new BasicPlumbingTermination<TestdataSolution>(false);
        termination.solvingEnded(mock(SolverScope.class));
        termination.terminateEarly();
        assertThat(termination.waitForRestartSolverDecision()).isFalse();
        assertThat(termination.isRunEndedByTerminateEarly()).isFalse();
    }

    @Test
    void terminateEarlyPreventingRestartIsEarly() {
        var termination = new BasicPlumbingTermination<TestdataSolution>(false);
        addNoopProblemChange(termination);
        termination.solvingEnded(mock(SolverScope.class));
        termination.terminateEarly();
        assertThat(termination.waitForRestartSolverDecision()).isFalse();
        assertThat(termination.isRunEndedByTerminateEarly()).isTrue();
    }

    @Test
    void daemonStoppedWhileIdleIsEarly() {
        var termination = new BasicPlumbingTermination<TestdataSolution>(true);
        termination.solvingEnded(mock(SolverScope.class));
        termination.terminateEarly();
        assertThat(termination.waitForRestartSolverDecision()).isFalse();
        assertThat(termination.isRunEndedByTerminateEarly()).isTrue();
    }

    @Test
    void problemChangeAfterRestartDecisionIsNotEarly() {
        var termination = new BasicPlumbingTermination<TestdataSolution>(false);
        termination.solvingEnded(mock(SolverScope.class));
        assertThat(termination.waitForRestartSolverDecision()).isFalse();
        // Another thread adds a problem change after the decision.
        addNoopProblemChange(termination);
        assertThat(termination.isRunEndedByTerminateEarly()).isFalse();
    }

    @Test
    void solvingStartedClearsTheAnswer() {
        var termination = new BasicPlumbingTermination<TestdataSolution>(false);
        termination.terminateEarly();
        termination.solvingEnded(mock(SolverScope.class));
        termination.solvingStarted();
        assertThat(termination.isRunEndedByTerminateEarly()).isFalse();
        assertThat(termination.isTerminateEarlyRequested()).isFalse();
    }

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
