package ai.timefold.solver.core.impl.solver.termination;

import ai.timefold.solver.core.impl.localsearch.decider.acceptor.simulatedannealing.SimulatedAnnealingAcceptor;
import ai.timefold.solver.core.impl.phase.Phase;
import ai.timefold.solver.core.impl.phase.scope.AbstractPhaseScope;
import ai.timefold.solver.core.impl.phase.scope.AbstractStepScope;

import org.jspecify.annotations.NullMarked;

/**
 * Determines when a {@link Phase} should stop.
 */
@NullMarked
public sealed interface PhaseTermination<Solution_>
        extends Termination<Solution_>
        permits AbstractPhaseTermination, MockablePhaseTermination, UniversalTermination {

    /**
     * @return false if the termination should be skipped on the given phase,
     *         when used as part of {@link AbstractCompositeTermination}.
     */
    @SuppressWarnings("rawtypes")
    default boolean isApplicableTo(Class<? extends AbstractPhaseScope> phaseScopeClass) {
        return true;
    }

    /**
     * Called by the {@link Phase} after every step and every move to determine if the search should stop.
     *
     * @return true if the search should terminate.
     */
    boolean isPhaseTerminated(AbstractPhaseScope<Solution_> phaseScope);

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
    double calculatePhaseTimeGradient(AbstractPhaseScope<Solution_> phaseScope);

    void phaseStarted(AbstractPhaseScope<Solution_> phaseScope);

    void stepStarted(AbstractStepScope<Solution_> stepScope);

    void stepEnded(AbstractStepScope<Solution_> stepScope);

    void phaseEnded(AbstractPhaseScope<Solution_> phaseScope);

    static <Solution_> PhaseTermination<Solution_> bridge(SolverTermination<Solution_> termination) {
        return new SolverBridgePhaseTermination<>(termination);
    }

}
