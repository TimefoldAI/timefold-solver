package ai.timefold.solver.core.impl.solver.termination;

import ai.timefold.solver.core.api.solver.Solver;
import ai.timefold.solver.core.impl.localsearch.decider.acceptor.simulatedannealing.SimulatedAnnealingAcceptor;
import ai.timefold.solver.core.impl.phase.Phase;
import ai.timefold.solver.core.impl.phase.event.PhaseLifecycleListener;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;

/**
 * Determines when a {@link Solver} or a {@link Phase} should stop.
 */
public sealed interface SolverTermination<Solution_>
        extends Termination<Solution_>, PhaseLifecycleListener<Solution_>
        permits AbstractSolverTermination, MockableSolverTermination {

    /**
     * Called by the {@link Solver} after every phase to determine if the search should stop.
     *
     * @param solverScope never null
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
     * @param solverScope never null
     * @return timeGradient t for which {@code 0.0 <= t <= 1.0 or -1.0} when it is not supported.
     *         At the start of a solver t is 0.0 and at the end t would be 1.0.
     */
    double calculateSolverTimeGradient(SolverScope<Solution_> solverScope);

    static <Solution_> Termination<Solution_> or(Termination<Solution_>... terminations) {
        return switch (terminations.length) {
            case 0 -> null;
            case 1 -> terminations[0];
            default -> new OrCompositeTermination<>(terminations);
        };
    }

    static <Solution_> Termination<Solution_> and(Termination<Solution_>... terminations) {
        return switch (terminations.length) {
            case 0 -> null;
            case 1 -> terminations[0];
            default -> new AndCompositeTermination<>(terminations);
        };
    }

    static <Solution_> Termination<Solution_> bridge(SolverTermination<Solution_> solverTermination) {
        return new PhaseToSolverTerminationBridge<>(solverTermination);
    }

}
