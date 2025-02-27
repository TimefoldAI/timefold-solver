package ai.timefold.solver.core.impl.solver.termination;

import ai.timefold.solver.core.impl.phase.scope.AbstractPhaseScope;
import ai.timefold.solver.core.impl.phase.scope.AbstractStepScope;

import org.jspecify.annotations.NullMarked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@NullMarked
abstract sealed class AbstractTermination<Solution_>
        implements Termination<Solution_>
        permits AbstractSolverTermination, DiminishedReturnsTermination, PhaseToSolverTerminationBridge, StepCountTermination,
        UnimprovedStepCountTermination {

    protected final transient Logger logger = LoggerFactory.getLogger(getClass());

    @Override
    public void phaseStarted(AbstractPhaseScope<Solution_> phaseScope) {
        // Override if needed.
    }

    protected static <Solution_> void phaseStarted(Termination<Solution_> termination, AbstractPhaseScope<Solution_> scope) {
        termination.phaseStarted(scope);
    }

    @Override
    public void stepStarted(AbstractStepScope<Solution_> stepScope) {
        // Override if needed.
    }

    protected static <Solution_> void stepStarted(Termination<Solution_> termination, AbstractStepScope<Solution_> scope) {
        termination.stepStarted(scope);
    }

    @Override
    public void stepEnded(AbstractStepScope<Solution_> stepScope) {
        // Override if needed.
    }

    protected static <Solution_> void stepEnded(Termination<Solution_> termination, AbstractStepScope<Solution_> scope) {
        termination.stepEnded(scope);
    }

    @Override
    public void phaseEnded(AbstractPhaseScope<Solution_> phaseScope) {
        // Override if needed.
    }

    protected static <Solution_> void phaseEnded(Termination<Solution_> termination, AbstractPhaseScope<Solution_> scope) {
        termination.phaseEnded(scope);
    }

}
