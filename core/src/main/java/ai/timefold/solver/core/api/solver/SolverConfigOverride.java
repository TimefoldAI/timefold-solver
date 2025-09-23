package ai.timefold.solver.core.api.solver;

import java.time.Duration;
import java.util.Objects;

import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.config.solver.termination.TerminationConfig;

import org.jspecify.annotations.NonNull;

/**
 * Includes settings to override default {@link ai.timefold.solver.core.api.solver.Solver} configuration.
 * <p>
 * This class provides an API to override solver termination settings. The following options are available:
 * <ul>
 * <li>Use {@link #withTerminationConfig(TerminationConfig)} to set a complete termination configuration</li>
 * <li>Use {@link #withTerminationSpentLimit(Duration)} and/or {@link #withTerminationUnimprovedSpentLimit(Duration)}
 * to set specific time limits independently or in combination</li>
 * </ul>
 * <p>
 * <strong>Important ordering constraint:</strong> If {@link #withTerminationConfig(TerminationConfig)} is used,
 * it must be called before any specific termination methods like {@link #withTerminationSpentLimit(Duration)}
 * or {@link #withTerminationUnimprovedSpentLimit(Duration)}. This prevents accidental override of previously
 * set specific configurations.
 *
 * @param <Solution_> the solution type, the class with the {@link PlanningSolution} annotation
 */
public final class SolverConfigOverride<Solution_> {

    private TerminationConfig terminationConfig = null;
    private boolean hasSpecificTerminationSettings = false;

    public TerminationConfig getTerminationConfig() {
        return terminationConfig;
    }

    /**
     * Sets the solver {@link TerminationConfig}, providing a base configuration that can be further
     * customized with specific termination methods.
     * <p>
     * After calling this method, additional specific termination methods can be chained to further
     * customize the configuration:
     * 
     * <pre>{@code
     * new SolverConfigOverride<MySolution>()
     *         .withTerminationConfig(new TerminationConfig())
     *         .withTerminationSpentLimit(Duration.ofMinutes(5))
     *         .withTerminationUnimprovedSpentLimit(Duration.ofMinutes(2));
     * }</pre>
     *
     * <p>
     * <strong>Important:</strong> This method must be called before any specific termination methods
     * like {@link #withTerminationSpentLimit(Duration)} or {@link #withTerminationUnimprovedSpentLimit(Duration)}.
     * <p>
     * Calling this method after specific termination settings have been applied will throw an exception
     * to prevent accidental override of those settings.
     *
     *
     * <pre>{@code
     * new SolverConfigOverride<MySolution>()
     *         .withTerminationSpentLimit(Duration.ofMinutes(5))
     *         .withTerminationConfig(new TerminationConfig()); // Will throw exception
     * }</pre>
     *
     * @param terminationConfig allows overriding the default termination config of {@link Solver}
     * @return this
     * @throws IllegalStateException if specific termination settings have already been applied
     */
    public @NonNull SolverConfigOverride<Solution_> withTerminationConfig(@NonNull TerminationConfig terminationConfig) {
        if (hasSpecificTerminationSettings) {
            throw new IllegalStateException("""
                    Cannot set terminationConfig after specific termination settings
                    (withTerminationSpentLimit or withTerminationUnimprovedSpentLimit) have been applied.
                    Maybe call withTerminationConfig() first, or use the TerminationConfig builder methods directly.""");
        }
        this.terminationConfig =
                Objects.requireNonNull(terminationConfig, "Invalid terminationConfig (null) given to SolverConfigOverride.");
        return this;
    }

    /**
     * Sets a time limit for the solver to run, creating or updating the default termination configuration.
     * <p>
     * This method sets the maximum duration the solver is allowed to run before terminating.
     * It can be used independently or in combination with {@link #withTerminationUnimprovedSpentLimit(Duration)}.
     * <p>
     * If no {@link TerminationConfig} has been set via {@link #withTerminationConfig(TerminationConfig)},
     * this method will create a new one. If a TerminationConfig already exists, this method will
     * update its spent limit setting.
     * <p>
     * Usage examples:
     * 
     * <pre>{@code
     * // Set only spent limit
     * new SolverConfigOverride<MySolution>()
     *         .withTerminationSpentLimit(Duration.ofMinutes(10));
     *
     * // Combine with unimproved spent limit
     * new SolverConfigOverride<MySolution>()
     *         .withTerminationSpentLimit(Duration.ofMinutes(10))
     *         .withTerminationUnimprovedSpentLimit(Duration.ofMinutes(3));
     *
     * // Use with base config
     * new SolverConfigOverride<MySolution>()
     *         .withTerminationConfig(new TerminationConfig())
     *         .withTerminationSpentLimit(Duration.ofMinutes(10));
     * }</pre>
     *
     * @param spentLimit the maximum duration the solver is allowed to run
     * @return this
     */
    public @NonNull SolverConfigOverride<Solution_> withTerminationSpentLimit(@NonNull Duration spentLimit) {
        if (this.terminationConfig == null) {
            this.terminationConfig = new TerminationConfig();
        }
        this.terminationConfig.setSpentLimit(spentLimit);
        this.hasSpecificTerminationSettings = true;
        return this;
    }

    /**
     * Sets a time limit for the solver to run without finding an improved solution, creating or updating
     * the default termination configuration.
     * <p>
     * This method sets the maximum duration the solver is allowed to run without improvement before terminating.
     * The solver will stop if it hasn't found a better solution within this time limit, even if the total
     * spent limit (if set) hasn't been reached yet.
     * <p>
     * This method can be used independently or in combination with {@link #withTerminationSpentLimit(Duration)}.
     * When used together, the solver will terminate when either condition is met (whichever comes first).
     * <p>
     * If no {@link TerminationConfig} has been set via {@link #withTerminationConfig(TerminationConfig)},
     * this method will create a new one. If a TerminationConfig already exists, this method will
     * update its unimproved spent limit setting.
     * <p>
     * Usage examples:
     * 
     * <pre>{@code
     * // Set only unimproved spent limit
     * new SolverConfigOverride<MySolution>()
     *         .withTerminationUnimprovedSpentLimit(Duration.ofMinutes(2));
     *
     * // Combine with total spent limit
     * new SolverConfigOverride<MySolution>()
     *         .withTerminationSpentLimit(Duration.ofMinutes(10))
     *         .withTerminationUnimprovedSpentLimit(Duration.ofMinutes(2));
     *
     * // Use with base config
     * new SolverConfigOverride<MySolution>()
     *         .withTerminationConfig(new TerminationConfig())
     *         .withTerminationUnimprovedSpentLimit(Duration.ofMinutes(2));
     * }</pre>
     *
     * @param unimprovedSpentLimit the maximum duration the solver is allowed to run without improvement
     * @return this
     */
    public @NonNull SolverConfigOverride<Solution_>
            withTerminationUnimprovedSpentLimit(@NonNull Duration unimprovedSpentLimit) {
        if (this.terminationConfig == null) {
            this.terminationConfig = new TerminationConfig();
        }
        this.terminationConfig.setUnimprovedSpentLimit(unimprovedSpentLimit);
        this.hasSpecificTerminationSettings = true;
        return this;
    }
}
