package ai.timefold.solver.core.api.solver.phase;

import ai.timefold.solver.core.config.solver.SolverConfig;

import org.jspecify.annotations.NullMarked;

/**
 * Identifies one phase of the solver.
 * Do not attempt to parse this class' {@link Object#toString() toString()},
 * it is not guaranteed to be stable.
 */
@NullMarked
public interface PhaseId {

    /**
     * @return the type of the phase
     */
    PhaseType type();

    /**
     * @return the index of the phase in {@link SolverConfig#getPhaseConfigList()}
     */
    int phaseIndex();

}
