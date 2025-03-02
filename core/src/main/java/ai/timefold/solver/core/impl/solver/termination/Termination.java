package ai.timefold.solver.core.impl.solver.termination;

import ai.timefold.solver.core.impl.phase.Phase;

import org.jspecify.annotations.NullMarked;

/**
 * Determines when a {@link Phase} should stop.
 * See {@link SolverTermination} for the termination that also supports stopping the solver.
 * Many terminations are used at both solver-level and phase-level.
 */
@NullMarked
public sealed interface Termination<Solution_>
        permits AbstractTermination, PhaseTermination, SolverTermination {

}
