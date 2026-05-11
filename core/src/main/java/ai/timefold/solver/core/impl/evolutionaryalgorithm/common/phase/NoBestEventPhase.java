package ai.timefold.solver.core.impl.evolutionaryalgorithm.common.phase;

import java.util.function.IntFunction;

import ai.timefold.solver.core.api.solver.event.EventProducerId;
import ai.timefold.solver.core.impl.phase.AbstractPhase;
import ai.timefold.solver.core.impl.phase.Phase;
import ai.timefold.solver.core.impl.phase.event.PhaseLifecycleListener;
import ai.timefold.solver.core.impl.phase.scope.AbstractPhaseScope;
import ai.timefold.solver.core.impl.phase.scope.AbstractStepScope;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;

/**
 * The phase receives an inner phase and disables any best solution events,
 * which is required when running the inner phase in the evolutionary process.
 * 
 * @param <Solution_> the solution type
 */
public final class NoBestEventPhase<Solution_> implements Phase<Solution_> {
    private final Phase<Solution_> innerPhase;
    private boolean previousState;

    public NoBestEventPhase(Phase<Solution_> innerPhase) {
        this.innerPhase = innerPhase;
        if (innerPhase instanceof AbstractPhase<Solution_> abstractPhase) {
            abstractPhase.disableLogging();
        }
    }

    @Override
    public void addPhaseLifecycleListener(PhaseLifecycleListener<Solution_> phaseLifecycleListener) {
        innerPhase.addPhaseLifecycleListener(phaseLifecycleListener);
    }

    @Override
    public void removePhaseLifecycleListener(PhaseLifecycleListener<Solution_> phaseLifecycleListener) {
        innerPhase.removePhaseLifecycleListener(phaseLifecycleListener);
    }

    @Override
    public void solve(SolverScope<Solution_> solverScope) {
        innerPhase.solve(solverScope);
    }

    @Override
    public IntFunction<EventProducerId> getEventProducerIdSupplier() {
        return innerPhase.getEventProducerIdSupplier();
    }

    @Override
    public void solvingStarted(SolverScope<Solution_> solverScope) {
        var solver = solverScope.getSolver();
        if (solver != null) {
            previousState = solver.getBestSolutionRecaller().isEnableUpdateEvents();
            solver.getBestSolutionRecaller().setEnableUpdateEvents(false);
        }
        innerPhase.solvingStarted(solverScope);
    }

    @Override
    public void solvingEnded(SolverScope<Solution_> solverScope) {
        var solver = solverScope.getSolver();
        if (solver != null) {
            solver.getBestSolutionRecaller().setEnableUpdateEvents(previousState);
        }
        innerPhase.solvingEnded(solverScope);
    }

    @Override
    public void phaseStarted(AbstractPhaseScope<Solution_> phaseScope) {
        innerPhase.phaseStarted(phaseScope);
    }

    @Override
    public void phaseEnded(AbstractPhaseScope<Solution_> phaseScope) {
        innerPhase.phaseEnded(phaseScope);
    }

    @Override
    public void stepStarted(AbstractStepScope<Solution_> stepScope) {
        // Do nothing
    }

    @Override
    public void stepEnded(AbstractStepScope<Solution_> stepScope) {
        // Do nothing
    }
}
