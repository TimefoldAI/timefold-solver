package ai.timefold.solver.core.impl.solver.termination;

import static ai.timefold.solver.core.impl.solver.termination.AbstractCompositeTerminationTest.mockPhaseTermination;
import static ai.timefold.solver.core.testutil.PlannerTestUtils.mockSolverScope;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.Offset.offset;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ai.timefold.solver.core.impl.phase.scope.AbstractPhaseScope;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;
import ai.timefold.solver.core.testdomain.TestdataSolution;

import org.junit.jupiter.api.Test;

class SolverBridgePhaseTerminationTest {

    @Test
    void solverLevelPhaseTerminationGivesThePhaseATimeGradient() {
        PhaseTermination<TestdataSolution> phaseOnlyTermination = mockPhaseTermination();
        var bridge = new SolverBridgePhaseTermination<>(
                new OrCompositeTermination<>(new TimeMillisSpentTermination<>(1000L), phaseOnlyTermination));
        SolverScope<TestdataSolution> solverScope = mockSolverScope();
        AbstractPhaseScope<TestdataSolution> phaseScope = mock(AbstractPhaseScope.class);
        when(phaseScope.getSolverScope()).thenReturn(solverScope);
        when(solverScope.calculateTimeMillisSpentUpToNow()).thenReturn(200L);
        when(phaseScope.calculatePhaseTimeMillisSpentUpToNow()).thenReturn(900L);

        when(phaseOnlyTermination.calculatePhaseTimeGradient(phaseScope)).thenReturn(0.6);
        assertThat(bridge.calculatePhaseTimeGradient(phaseScope)).isEqualTo(0.6, offset(0.0));
        // A solver-level time limit measures the time of the solver, not of the phase.
        when(phaseOnlyTermination.calculatePhaseTimeGradient(phaseScope)).thenReturn(-1.0);
        assertThat(bridge.calculatePhaseTimeGradient(phaseScope)).isEqualTo(0.2, offset(0.0));
    }

    @Test
    void noTimeGradient() {
        var bridge = new SolverBridgePhaseTermination<TestdataSolution>(new BasicPlumbingTermination<>(false));
        SolverScope<TestdataSolution> solverScope = mockSolverScope();
        AbstractPhaseScope<TestdataSolution> phaseScope = mock(AbstractPhaseScope.class);
        when(phaseScope.getSolverScope()).thenReturn(solverScope);
        assertThat(bridge.calculatePhaseTimeGradient(phaseScope)).isEqualTo(-1.0, offset(0.0));
    }

}
