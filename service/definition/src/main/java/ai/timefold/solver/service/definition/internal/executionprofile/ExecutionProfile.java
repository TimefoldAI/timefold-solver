package ai.timefold.solver.service.definition.internal.executionprofile;

import java.util.Map;

/**
 * A named, predefined runtime configuration a run can be started with.
 */
public interface ExecutionProfile {

    /**
     * Stable, unique identifier of the profile.
     */
    String id();

    /**
     * Human readable name of the profile, for display in user interfaces. Unlike {@link #id()} this is not a stable key:
     * it may be changed or localized without breaking existing references.
     */
    String name();

    /**
     * Human readable description of the profile.
     */
    String description();

    /**
     * Reads the values this profile recognizes from the run's options (as supplied via {@code RunConfiguration.options}),
     * validates them, and maps them to environment variables injected into the solver pod. The profile picks out only the
     * keys it recognizes and ignores the rest, since the options map is shared with other run configuration. Keys must
     * be valid environment-variable names. If two activated profiles map to the same environment variable, the run is
     * rejected. Defaults to no environment variables.
     */
    default Map<String, String> toEnvironment(Map<String, String> options) {
        return Map.of();
    }
}
