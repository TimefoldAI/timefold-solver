package ai.timefold.solver.core.impl.solver.termination;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

abstract class AbstractCompositeTerminationTest {

    abstract void solveTermination();

    abstract void phaseTermination();

    abstract void phaseTerminationInapplicable();

    abstract void calculateSolverTimeGradientTest();

    abstract void calculatePhaseTimeGradientTest();

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
