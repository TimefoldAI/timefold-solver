package ai.timefold.solver.core.config.score.director;

/**
 * Controls how constraints are profiled.
 * Enabling profiling have a minor performance impact.
 */
public enum ConstraintProfilingMode {
    /**
     * Disables profiling.
     */
    NONE,

    /**
     * Profile by the method an operation was defined in.
     */
    BY_METHOD,

    /**
     * Profile by the line an operation was defined on.
     */
    BY_LINE
}
