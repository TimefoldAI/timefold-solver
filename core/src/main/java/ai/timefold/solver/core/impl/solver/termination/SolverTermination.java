package ai.timefold.solver.core.impl.solver.termination;

import ai.timefold.solver.core.api.solver.Solver;
import ai.timefold.solver.core.impl.localsearch.decider.acceptor.simulatedannealing.SimulatedAnnealingAcceptor;
import ai.timefold.solver.core.impl.solver.event.SolverLifecycleListener;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;

import org.jspecify.annotations.NullMarked;

/**
 * Determines when a {@link Solver} should stop.
 */
@NullMarked
public sealed interface SolverTermination<Solution_>
        extends Termination<Solution_>, SolverLifecycleListener<Solution_>
        permits MockableSolverTermination, UniversalTermination {

    /**
     * Called by the {@link Solver} after every phase to determine if the search should stop.
     *
     * @return true if the search should terminate.
     */
    boolean isSolverTerminated(SolverScope<Solution_> solverScope);

    /**
     * A timeGradient is a relative estimate of how long the search will continue.
     * <p>
     * Clients that use a timeGradient should cache it at the start of a single step
     * because some implementations are not time-stable.
     * <p>
     * If a timeGradient cannot be calculated, it should return -1.0.
     * Several implementations (such a {@link SimulatedAnnealingAcceptor}) require a correctly implemented timeGradient.
     * <p>
     * A Termination's timeGradient can be requested after they are terminated, so implementations
     * should be careful not to return a timeGradient above 1.0.
     *
     * @return timeGradient t for which {@code 0.0 <= t <= 1.0 or -1.0} when it is not supported.
     *         At the start of a solver t is 0.0 and at the end t would be 1.0.
     */
    double calculateSolverTimeGradient(SolverScope<Solution_> solverScope);

}
