package ai.timefold.solver.service.definition.api.executionprofile;

/**
 * A named, predefined runtime configuration a run can be started with.
 * <p>
 * Execution profiles belong to the solver service: they describe how a run executes (diagnostics, logging, profiling, ...).
 * Implementations are discovered as CDI beans (see {@link ExecutionProfileRegistry}), so a new profile can be added simply by
 * providing a new implementation - no central registry needs to be edited. A run may activate several profiles at once.
 */
public interface ExecutionProfile {

    /**
     * Stable identifier of the profile, used in APIs and permissions. Must be unique across all implementations.
     */
    String name();

    /**
     * Human readable description of the profile.
     */
    String description();
}
