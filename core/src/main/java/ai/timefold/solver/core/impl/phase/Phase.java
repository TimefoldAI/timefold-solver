package ai.timefold.solver.core.impl.phase;

import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.solver.Solver;
import ai.timefold.solver.core.impl.phase.event.PhaseLifecycleListener;
import ai.timefold.solver.core.impl.phase.scope.AbstractPhaseScope;
import ai.timefold.solver.core.impl.phase.scope.AbstractStepScope;
import ai.timefold.solver.core.impl.solver.DefaultSolver;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;

/**
 * A phase of a {@link Solver}.
 *
 * @param <Solution_> the solution type, the class with the {@link PlanningSolution} annotation
 * @see AbstractPhase
 */
public interface Phase<Solution_> extends PhaseLifecycleListener<Solution_> {

    /**
     * Add a {@link PhaseLifecycleListener} that is only notified
     * of the {@link PhaseLifecycleListener#phaseStarted(AbstractPhaseScope) phase}
     * and the {@link PhaseLifecycleListener#stepStarted(AbstractStepScope) step} starting/ending events from this phase
     * (and the {@link PhaseLifecycleListener#solvingStarted(SolverScope) solving} events too of course).
     * <p>
     * To get notified for all phases, use {@link DefaultSolver#addPhaseLifecycleListener(PhaseLifecycleListener)} instead.
     *
     * @param phaseLifecycleListener never null
     */
    void addPhaseLifecycleListener(PhaseLifecycleListener<Solution_> phaseLifecycleListener);

    /**
     * @param phaseLifecycleListener never null
     * @see #addPhaseLifecycleListener(PhaseLifecycleListener)
     */
    void removePhaseLifecycleListener(PhaseLifecycleListener<Solution_> phaseLifecycleListener);

    void solve(SolverScope<Solution_> solverScope);

    /**
     * Checks if a phase returns an initialized solution.
     *
     * @return true if the phase returns an initialized solution.
     */
    boolean isInitializationPhase();
}
