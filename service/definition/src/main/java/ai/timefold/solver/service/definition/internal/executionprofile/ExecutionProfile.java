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
     * Reads the values this profile cares about from the run's options (as supplied via
     * {@code RunConfiguration.options}), applying defaults, generating values for absent inputs, and validating them.
     * The profile picks out only the keys it recognizes and ignores the rest, since the options map is shared with other
     * run configuration. Called once at submit time; the returned values are persisted with the run so it stays
     * reproducible. Defaults to no parameters.
     */
    default Map<String, String> resolveParameters(Map<String, String> options) {
        return Map.of();
    }

    /**
     * Maps the resolved parameter values (from {@link #resolveParameters(Map)}) to environment variables injected into the
     * solver pod. Keys must be valid environment-variable names. When multiple profiles are activated and define the same
     * key, the resulting value is unspecified. Defaults to no environment variables.
     */
    default Map<String, String> toEnvironment(Map<String, String> resolvedParameters) {
        return Map.of();
    }
}
