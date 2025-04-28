package ai.timefold.solver.core.impl.solver.termination;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.Offset.offset;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;

import ai.timefold.solver.core.impl.phase.scope.AbstractPhaseScope;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;
import ai.timefold.solver.core.testdomain.TestdataSolution;

import org.junit.jupiter.api.Test;

class AndCompositeTerminationTest extends AbstractCompositeTerminationTest {

    @Test
    @Override
    void solveTermination() {
        SolverTermination<TestdataSolution> termination1 = mock(MockableSolverTermination.class);
        SolverTermination<TestdataSolution> termination2 = mock(MockableSolverTermination.class);
        SolverTermination<TestdataSolution> compositeTermination = new AndCompositeTermination<>(termination1, termination2);
        SolverScope<TestdataSolution> solverScope = mock(SolverScope.class);

        when(termination1.isSolverTerminated(solverScope)).thenReturn(false);
        when(termination2.isSolverTerminated(solverScope)).thenReturn(false);
        assertThat(compositeTermination.isSolverTerminated(solverScope)).isFalse();

        when(termination1.isSolverTerminated(solverScope)).thenReturn(true);
        when(termination2.isSolverTerminated(solverScope)).thenReturn(false);
        assertThat(compositeTermination.isSolverTerminated(solverScope)).isFalse();

        when(termination1.isSolverTerminated(solverScope)).thenReturn(false);
        when(termination2.isSolverTerminated(solverScope)).thenReturn(true);
        assertThat(compositeTermination.isSolverTerminated(solverScope)).isFalse();

        when(termination1.isSolverTerminated(solverScope)).thenReturn(true);
        when(termination2.isSolverTerminated(solverScope)).thenReturn(true);
        assertThat(compositeTermination.isSolverTerminated(solverScope)).isTrue();
    }

    @Test
    @Override
    void phaseTermination() {
        PhaseTermination<TestdataSolution> termination1 = mockPhaseTermination();
        PhaseTermination<TestdataSolution> termination2 = mockPhaseTermination();
        UniversalTermination<TestdataSolution> compositeTermination =
                new AndCompositeTermination<>(Arrays.asList(termination1, termination2));
        AbstractPhaseScope<TestdataSolution> phaseScope = mock(AbstractPhaseScope.class);

        when(termination1.isPhaseTerminated(phaseScope)).thenReturn(false);
        when(termination2.isPhaseTerminated(phaseScope)).thenReturn(false);
        assertThat(compositeTermination.isPhaseTerminated(phaseScope)).isFalse();

        when(termination1.isPhaseTerminated(phaseScope)).thenReturn(true);
        when(termination2.isPhaseTerminated(phaseScope)).thenReturn(false);
        assertThat(compositeTermination.isPhaseTerminated(phaseScope)).isFalse();

        when(termination1.isPhaseTerminated(phaseScope)).thenReturn(false);
        when(termination2.isPhaseTerminated(phaseScope)).thenReturn(true);
        assertThat(compositeTermination.isPhaseTerminated(phaseScope)).isFalse();

        when(termination1.isPhaseTerminated(phaseScope)).thenReturn(true);
        when(termination2.isPhaseTerminated(phaseScope)).thenReturn(true);
        assertThat(compositeTermination.isPhaseTerminated(phaseScope)).isTrue();
    }

    @Test
    @Override
    void phaseTerminationInapplicable() {
        PhaseTermination<TestdataSolution> inapplicableTermination = mockPhaseTermination(false);
        PhaseTermination<TestdataSolution> supportedTermination = mockPhaseTermination(true);
        UniversalTermination<TestdataSolution> compositeTermination =
                new AndCompositeTermination<>(Arrays.asList(inapplicableTermination, supportedTermination));
        AbstractPhaseScope<TestdataSolution> phaseScope = mock(AbstractPhaseScope.class);

        when(inapplicableTermination.isPhaseTerminated(phaseScope)).thenReturn(false);
        when(supportedTermination.isPhaseTerminated(phaseScope)).thenReturn(false);
        assertThat(compositeTermination.isPhaseTerminated(phaseScope)).isFalse();

        when(inapplicableTermination.isPhaseTerminated(phaseScope)).thenReturn(true);
        when(supportedTermination.isPhaseTerminated(phaseScope)).thenReturn(false);
        assertThat(compositeTermination.isPhaseTerminated(phaseScope)).isFalse();

        when(inapplicableTermination.isPhaseTerminated(phaseScope)).thenReturn(false);
        when(supportedTermination.isPhaseTerminated(phaseScope)).thenReturn(true);
        assertThat(compositeTermination.isPhaseTerminated(phaseScope)).isTrue();

        when(inapplicableTermination.isPhaseTerminated(phaseScope)).thenReturn(true);
        when(supportedTermination.isPhaseTerminated(phaseScope)).thenReturn(true);
        assertThat(compositeTermination.isPhaseTerminated(phaseScope)).isTrue();
    }

    @Test
    @Override
    void calculateSolverTimeGradientTest() {
        SolverTermination<TestdataSolution> termination1 = mock(MockableSolverTermination.class);
        SolverTermination<TestdataSolution> termination2 = mock(MockableSolverTermination.class);
        SolverTermination<TestdataSolution> compositeTermination = new AndCompositeTermination<>(termination1, termination2);
        SolverScope<TestdataSolution> solverScope = mock(SolverScope.class);

        when(termination1.calculateSolverTimeGradient(solverScope)).thenReturn(0.0);
        when(termination2.calculateSolverTimeGradient(solverScope)).thenReturn(0.0);
        // min(0.0,0.0) = 0.0
        assertThat(compositeTermination.calculateSolverTimeGradient(solverScope)).isEqualTo(0.0, offset(0.0));

        when(termination1.calculateSolverTimeGradient(solverScope)).thenReturn(0.5);
        when(termination2.calculateSolverTimeGradient(solverScope)).thenReturn(0.0);
        // min(0.5,0.0) = 0.0
        assertThat(compositeTermination.calculateSolverTimeGradient(solverScope)).isEqualTo(0.0, offset(0.0));

        when(termination1.calculateSolverTimeGradient(solverScope)).thenReturn(0.0);
        when(termination2.calculateSolverTimeGradient(solverScope)).thenReturn(0.5);
        // min(0.0,0.5) = 0.0
        assertThat(compositeTermination.calculateSolverTimeGradient(solverScope)).isEqualTo(0.0, offset(0.0));

        when(termination1.calculateSolverTimeGradient(solverScope)).thenReturn(-1.0);
        when(termination2.calculateSolverTimeGradient(solverScope)).thenReturn(-1.0);
        // Negative time gradient values are unsupported and ignored, min(unsupported,unsupported) = 1.0 (default)
        assertThat(compositeTermination.calculateSolverTimeGradient(solverScope)).isEqualTo(1.0, offset(0.0));

        when(termination1.calculateSolverTimeGradient(solverScope)).thenReturn(0.5);
        when(termination2.calculateSolverTimeGradient(solverScope)).thenReturn(-1.0);
        // Negative time gradient values are unsupported and ignored, min(0.5,unsupported) = 0.5
        assertThat(compositeTermination.calculateSolverTimeGradient(solverScope)).isEqualTo(0.5, offset(0.0));

        when(termination1.calculateSolverTimeGradient(solverScope)).thenReturn(-1.0);
        when(termination2.calculateSolverTimeGradient(solverScope)).thenReturn(0.5);
        // Negative time gradient values are unsupported and ignored, min(unsupported,0.5) = 0.5
        assertThat(compositeTermination.calculateSolverTimeGradient(solverScope)).isEqualTo(0.5, offset(0.0));
    }

    @Test
    @Override
    void calculatePhaseTimeGradientTest() {
        PhaseTermination<TestdataSolution> termination1 = mockPhaseTermination();
        PhaseTermination<TestdataSolution> termination2 = mockPhaseTermination();
        UniversalTermination<TestdataSolution> compositeTermination =
                new AndCompositeTermination<>(Arrays.asList(termination1, termination2));
        AbstractPhaseScope<TestdataSolution> phaseScope = mock(AbstractPhaseScope.class);

        when(termination1.calculatePhaseTimeGradient(phaseScope)).thenReturn(0.0);
        when(termination2.calculatePhaseTimeGradient(phaseScope)).thenReturn(0.0);
        // min(0.0,0.0) = 0.0
        assertThat(compositeTermination.calculatePhaseTimeGradient(phaseScope)).isEqualTo(0.0, offset(0.0));

        when(termination1.calculatePhaseTimeGradient(phaseScope)).thenReturn(0.5);
        when(termination2.calculatePhaseTimeGradient(phaseScope)).thenReturn(0.0);
        // min(0.5,0.0) = 0.0
        assertThat(compositeTermination.calculatePhaseTimeGradient(phaseScope)).isEqualTo(0.0, offset(0.0));

        when(termination1.calculatePhaseTimeGradient(phaseScope)).thenReturn(0.0);
        when(termination2.calculatePhaseTimeGradient(phaseScope)).thenReturn(0.5);
        // min(0.0,0.5) = 0.0
        assertThat(compositeTermination.calculatePhaseTimeGradient(phaseScope)).isEqualTo(0.0, offset(0.0));

        when(termination1.calculatePhaseTimeGradient(phaseScope)).thenReturn(-1.0);
        when(termination2.calculatePhaseTimeGradient(phaseScope)).thenReturn(-1.0);
        // Negative time gradient values are unsupported and ignored, min(unsupported,unsupported) = 1.0 (default)
        assertThat(compositeTermination.calculatePhaseTimeGradient(phaseScope)).isEqualTo(1.0, offset(0.0));

        when(termination1.calculatePhaseTimeGradient(phaseScope)).thenReturn(0.5);
        when(termination2.calculatePhaseTimeGradient(phaseScope)).thenReturn(-1.0);
        // Negative time gradient values are unsupported and ignored, min(0.5,unsupported) = 0.5
        assertThat(compositeTermination.calculatePhaseTimeGradient(phaseScope)).isEqualTo(0.5, offset(0.0));

        when(termination1.calculatePhaseTimeGradient(phaseScope)).thenReturn(-1.0);
        when(termination2.calculatePhaseTimeGradient(phaseScope)).thenReturn(0.5);
        // Negative time gradient values are unsupported and ignored, min(unsupported,0.5) = 0.5
        assertThat(compositeTermination.calculatePhaseTimeGradient(phaseScope)).isEqualTo(0.5, offset(0.0));
    }

    @Test
    @Override
    void calculatePhaseTimeGradientInapplicableTest() {
        PhaseTermination<TestdataSolution> inapplicableTermination = mockPhaseTermination(false);
        PhaseTermination<TestdataSolution> supportedTermination = mockPhaseTermination(true);
        UniversalTermination<TestdataSolution> compositeTermination =
                new AndCompositeTermination<>(Arrays.asList(inapplicableTermination, supportedTermination));
        AbstractPhaseScope<TestdataSolution> phaseScope = mock(AbstractPhaseScope.class);

        when(inapplicableTermination.calculatePhaseTimeGradient(phaseScope)).thenReturn(0.0);
        when(supportedTermination.calculatePhaseTimeGradient(phaseScope)).thenReturn(0.0);
        // min(0.0,0.0) = 0.0
        assertThat(compositeTermination.calculatePhaseTimeGradient(phaseScope)).isEqualTo(0.0, offset(0.0));

        when(inapplicableTermination.calculatePhaseTimeGradient(phaseScope)).thenReturn(0.5);
        when(supportedTermination.calculatePhaseTimeGradient(phaseScope)).thenReturn(0.0);
        // min(0.5,0.0) = 0.0
        assertThat(compositeTermination.calculatePhaseTimeGradient(phaseScope)).isEqualTo(0.0, offset(0.0));

        when(inapplicableTermination.calculatePhaseTimeGradient(phaseScope)).thenReturn(0.0);
        when(supportedTermination.calculatePhaseTimeGradient(phaseScope)).thenReturn(0.5);
        // min(0.0,0.5) = 0.0
        assertThat(compositeTermination.calculatePhaseTimeGradient(phaseScope)).isEqualTo(0.5, offset(0.0));

        when(inapplicableTermination.calculatePhaseTimeGradient(phaseScope)).thenReturn(-1.0);
        when(supportedTermination.calculatePhaseTimeGradient(phaseScope)).thenReturn(-1.0);
        // Negative time gradient values are unsupported and ignored, min(unsupported,unsupported) = 1.0 (default)
        assertThat(compositeTermination.calculatePhaseTimeGradient(phaseScope)).isEqualTo(1.0, offset(0.0));

        when(inapplicableTermination.calculatePhaseTimeGradient(phaseScope)).thenReturn(0.5);
        when(supportedTermination.calculatePhaseTimeGradient(phaseScope)).thenReturn(-1.0);
        // Negative time gradient values are unsupported and ignored = 1.0 (default)
        assertThat(compositeTermination.calculatePhaseTimeGradient(phaseScope)).isEqualTo(1.0, offset(0.0));

        when(inapplicableTermination.calculatePhaseTimeGradient(phaseScope)).thenReturn(-1.0);
        when(supportedTermination.calculatePhaseTimeGradient(phaseScope)).thenReturn(0.5);
        // Negative time gradient values are unsupported and ignored
        assertThat(compositeTermination.calculatePhaseTimeGradient(phaseScope)).isEqualTo(0.5, offset(0.0));
    }

}
