package ai.timefold.solver.core.impl.solver.termination;

import ai.timefold.solver.core.impl.phase.scope.AbstractPhaseScope;
import ai.timefold.solver.core.impl.phase.scope.AbstractStepScope;

import org.jspecify.annotations.NullMarked;

@NullMarked
abstract sealed class AbstractPhaseTermination<Solution_>
        extends AbstractTermination<Solution_>
        implements PhaseTermination<Solution_>
        permits DiminishedReturnsTermination, SolverBridgePhaseTermination, StepCountTermination,
        UnimprovedStepCountTermination {

    @Override
    public void phaseStarted(AbstractPhaseScope<Solution_> phaseScope) {
        // Override if needed.
    }

    @Override
    public void stepStarted(AbstractStepScope<Solution_> stepScope) {
        // Override if needed.
    }

    @Override
    public void stepEnded(AbstractStepScope<Solution_> stepScope) {
        // Override if needed.
    }

    @Override
    public void phaseEnded(AbstractPhaseScope<Solution_> phaseScope) {
        // Override if needed.
    }

}
