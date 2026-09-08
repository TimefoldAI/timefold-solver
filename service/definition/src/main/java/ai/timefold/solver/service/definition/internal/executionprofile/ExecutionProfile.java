package ai.timefold.solver.service.definition.internal.executionprofile;

import java.util.Map;

/**
 * A named, predefined runtime configuration a run can be started with.
 * <p>
 * Execution profiles describe how a run executes (diagnostics, logging, profiling, ...). This is an internal contract:
 * model developers are not expected to implement or reference it. Implementations are provided by the solver service and
 * the platform, and are discovered via {@link java.util.ServiceLoader}, so adding a new profile does not require editing
 * any central registry. A run may activate several profiles at once.
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

    /**
     * Additional configuration contributed by this profile, applied to the run's environment - each entry is injected as
     * an environment variable into the solver pod. Keys must be valid environment-variable names. When multiple profiles
     * are activated and define the same key, the resulting value is unspecified. Defaults to no extra configuration.
     */
    default Map<String, String> properties() {
        return Map.of();
    }
}
