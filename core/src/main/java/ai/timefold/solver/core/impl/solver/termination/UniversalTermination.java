package ai.timefold.solver.core.impl.solver.termination;

import java.util.Collections;
import java.util.List;

import ai.timefold.solver.core.api.solver.Solver;
import ai.timefold.solver.core.impl.phase.Phase;
import ai.timefold.solver.core.impl.phase.event.PhaseLifecycleListener;
import ai.timefold.solver.core.impl.phase.scope.AbstractPhaseScope;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Determines when a {@link Solver} or a {@link Phase} should stop.
 */
@NullMarked
public sealed interface UniversalTermination<Solution_>
        extends PhaseTermination<Solution_>, SolverTermination<Solution_>, PhaseLifecycleListener<Solution_>
        permits AbstractUniversalTermination {

    /**
     * @return List of {@link PhaseTermination}s that are part of this termination.
     *         The list is never null and is unmodifiable.
     *         If this termination is not a {@link AbstractCompositeTermination}, it returns an empty list.
     */
    default List<PhaseTermination<Solution_>> getPhaseTerminationList() {
        return Collections.emptyList();
    }

    /**
     * @return List of {@link Termination}s that are part of this termination,
     *         which are not supported by the given phase type.
     *         If this termination is not a {@link AbstractCompositeTermination}, it returns an empty list.
     *         The list is unmodifiable.
     */
    default List<Termination<Solution_>> getUnsupportedTerminationList(AbstractPhaseScope<Solution_> phaseScope) {
        return getPhaseTerminationList().stream()
                .filter(f -> !f.isSupported(phaseScope))
                .map(t -> (Termination<Solution_>) t)
                .toList();
    }

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

}
