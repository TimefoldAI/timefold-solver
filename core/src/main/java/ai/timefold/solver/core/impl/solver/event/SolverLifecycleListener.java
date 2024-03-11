package ai.timefold.solver.core.impl.solver.event;

import java.util.EventListener;

import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;

/**
 * @param <Solution_> the solution type, the class with the {@link PlanningSolution} annotation
 * @see SolverLifecycleListenerAdapter
 */
public interface SolverLifecycleListener<Solution_> extends EventListener {

    void solvingStarted(SolverScope<Solution_> solverScope);

    void solvingEnded(SolverScope<Solution_> solverScope);

    /**
     * Invoked in case of an exception in the {@link ai.timefold.solver.core.api.solver.Solver} run. In that case,
     * the {@link #solvingEnded(SolverScope)} is never called.
     * For internal purposes only.
     */
    default void solvingError(SolverScope<Solution_> solverScope, Exception exception) {
        // no-op
    }
}
