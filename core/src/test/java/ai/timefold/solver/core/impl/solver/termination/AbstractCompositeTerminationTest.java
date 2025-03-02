package ai.timefold.solver.core.impl.solver.termination;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;

abstract class AbstractCompositeTerminationTest {

    @Test
    abstract void solveTermination();

    @Test
    abstract void phaseTermination();

    @Test
    abstract void phaseTerminationInapplicable();

    @Test
    abstract void calculateSolverTimeGradientTest();

    @Test
    abstract void calculatePhaseTimeGradientTest();

    @Test
    abstract void calculatePhaseTimeGradientInapplicableTest();

    protected static <Solution_> PhaseTermination<Solution_> mockPhaseTermination() {
        return mockPhaseTermination(true);
    }

    protected static <Solution_> PhaseTermination<Solution_> mockPhaseTermination(boolean isApplicableToPhase) {
        PhaseTermination<Solution_> termination = mock(MockablePhaseTermination.class);
        doReturn(isApplicableToPhase).when(termination).isApplicableTo(any());
        return termination;
    }

}
