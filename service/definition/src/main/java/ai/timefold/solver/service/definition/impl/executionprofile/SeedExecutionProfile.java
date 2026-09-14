package ai.timefold.solver.service.definition.impl.executionprofile;

import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

import ai.timefold.solver.service.definition.internal.executionprofile.ExecutionProfile;

/**
 * Runs the solver with a fixed random seed, making a run reproducible.
 * <p>
 * The seed is an optional {@code seed} parameter; when it is not supplied, a random seed is generated and persisted with
 * the run so the exact seed used can be read back and replayed. The seed is applied by mapping it to the Timefold Quarkus
 * property {@code quarkus.timefold.solver.random-seed} (via its environment-variable form), which the solver pod applies to
 * its {@code SolverConfig} at startup.
 */
public final class SeedExecutionProfile implements ExecutionProfile {

    static final String PARAMETER_SEED = "seed";

    /**
     * Environment-variable form of {@code quarkus.timefold.solver.default.random-seed}, consumed by the Timefold Quarkus
     * extension. The {@code default} segment is the solver name ({@code TimefoldRuntimeConfig.DEFAULT_SOLVER_NAME}); it is
     * correct for the usual single, unnamed solver a model defines. Note this property lives under a solver-name-keyed map,
     * so injecting it purely via an environment variable may not be honored by SmallRye - see the profile's notes.
     */
    static final String ENV_QUARKUS_RANDOM_SEED = "QUARKUS_TIMEFOLD_SOLVER_DEFAULT_RANDOM_SEED";

    @Override
    public String id() {
        return "seed";
    }

    @Override
    public String name() {
        return "Fixed random seed";
    }

    @Override
    public String description() {
        return "Runs the solver with a fixed random seed for reproducible results. "
                + "A random seed is generated and recorded when none is supplied.";
    }

    @Override
    public Map<String, String> resolveParameters(Map<String, String> options) {
        String seed = options == null ? null : options.get(PARAMETER_SEED);
        if (seed == null) {
            return Map.of(PARAMETER_SEED, Long.toString(ThreadLocalRandom.current().nextLong()));
        }
        try {
            Long.parseLong(seed);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                    "Execution profile '" + id() + "' requires option '" + PARAMETER_SEED + "' to be a long, but was: "
                            + seed);
        }
        return Map.of(PARAMETER_SEED, seed);
    }

    @Override
    public Map<String, String> toEnvironment(Map<String, String> resolvedParameters) {
        String seed = resolvedParameters.get(PARAMETER_SEED);
        if (seed == null) {
            return Map.of();
        }
        return Map.of(ENV_QUARKUS_RANDOM_SEED, seed);
    }
}
