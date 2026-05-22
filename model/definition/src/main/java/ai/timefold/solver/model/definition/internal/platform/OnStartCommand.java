package ai.timefold.solver.model.definition.internal.platform;

/**
 * Command that is expected to be executed by solver worker upon start.
 * <p>
 * The command is passed to the model pod via environment variable {@link EnvironmentVars#ENV_TIMEFOLD_ON_START_COMMAND}.
 */
public enum OnStartCommand {

    VALIDATE_COMPUTE_SOLVE,
    SOLVE, // expects the dataset to be already validated and computed
    VALIDATE_COMPUTE,
    IDLE
}
