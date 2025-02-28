package ai.timefold.solver.core.impl.solver.termination;

import ai.timefold.solver.core.api.solver.Solver;
import ai.timefold.solver.core.impl.phase.Phase;
import ai.timefold.solver.core.impl.phase.event.PhaseLifecycleListener;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Determines when a {@link Solver} or a {@link Phase} should stop.
 */
@NullMarked
public sealed interface UniversalTermination<Solution_>
        extends PhaseTermination<Solution_>, SolverTermination<Solution_>, PhaseLifecycleListener<Solution_>
        permits AbstractUniversalTermination {

    @SafeVarargs
    static <Solution_> @Nullable UniversalTermination<Solution_> or(Termination<Solution_>... terminations) {
        if (terminations.length == 0) {
            return null;
        }
        return new OrCompositeTermination<>(terminations);
    }

    @SafeVarargs
    static <Solution_> @Nullable UniversalTermination<Solution_> and(Termination<Solution_>... terminations) {
        if (terminations.length == 0) {
            return null;
        }
        return new AndCompositeTermination<>(terminations);
    }

    static <Solution_> UniversalTermination<Solution_> bridge(Termination<Solution_> termination) {
        if (termination instanceof UniversalTermination<Solution_> universalTermination) {
            return universalTermination;
        } else if (termination instanceof SolverTermination<Solution_> solverTermination) {
            return new SolverToUniversalTerminationBridge<>(solverTermination);
        } else {
            throw new IllegalArgumentException("Impossible state: The termination (%s) is neither %s nor %s."
                    .formatted(termination, UniversalTermination.class.getSimpleName(),
                            SolverTermination.class.getSimpleName()));
        }
    }

}
