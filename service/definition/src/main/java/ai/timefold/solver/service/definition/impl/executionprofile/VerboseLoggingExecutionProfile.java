package ai.timefold.solver.service.definition.impl.executionprofile;

import java.util.Locale;
import java.util.Map;

import ai.timefold.solver.service.definition.internal.executionprofile.ExecutionProfile;
import ai.timefold.solver.service.definition.internal.platform.EnvironmentVars;

/**
 * Captures verbose solver logs into a file that is collected as a run artifact.
 * <p>
 * The profile takes no run options. It maps to a fixed set of Quarkus logging environment variables that enable a file
 * appender, raise the {@code ai.timefold.solver} category to {@code DEBUG}, and write into this profile's own subdirectory of
 * the execution-profile artifacts directory ({@link EnvironmentVars#DEFAULT_EXECUTION_PROFILE_DIR}). Whatever ends up there is
 * bundled and uploaded when the run finishes (see {@code ExecutionProfileModelPostProcessor} in the enterprise worker).
 * <p>
 * The extra verbosity is confined to the uploaded file: raising a category to {@code DEBUG} makes those records reach every
 * handler, and Quarkus' console handler defaults to level {@code ALL}, so without intervention the {@code DEBUG} lines would
 * also appear on stdout and inflate the pod's normal log stream. This profile therefore pins the console handler to
 * {@code INFO}, leaving the live stdout stream unchanged while the file captures the {@code DEBUG} detail.
 * <p>
 * The log volume is bounded by Quarkus' built-in size-based rotation: {@link #MAX_FILE_SIZE} per segment and at most
 * {@link #MAX_BACKUP_INDEX} rotated segments, giving a fixed ceiling that does not grow with run duration. These limits are
 * fixed constants of this specification, not tunable per run.
 */
public final class VerboseLoggingExecutionProfile implements ExecutionProfile {

    static final String ID = "verbose-logging";

    /** File name of the log, written inside {@code <artifacts-dir>/<id>/}. */
    static final String LOG_FILE_NAME = "solver-debug.log";

    /** Solver category raised to {@code DEBUG}; never a blanket root-logger bump. */
    static final String DEBUG_CATEGORY = "ai.timefold.solver";

    /** Maximum size of a single log segment before it is rotated. */
    static final String MAX_FILE_SIZE = "10M";

    /** Maximum number of rotated segments kept alongside the active log. */
    static final String MAX_BACKUP_INDEX = "2";

    static final String ENV_QUARKUS_LOG_FILE_ENABLE = "QUARKUS_LOG_FILE_ENABLE";
    static final String ENV_QUARKUS_LOG_FILE_PATH = "QUARKUS_LOG_FILE_PATH";
    static final String ENV_QUARKUS_LOG_FILE_ROTATION_MAX_FILE_SIZE = "QUARKUS_LOG_FILE_ROTATION_MAX_FILE_SIZE";
    static final String ENV_QUARKUS_LOG_FILE_ROTATION_MAX_BACKUP_INDEX = "QUARKUS_LOG_FILE_ROTATION_MAX_BACKUP_INDEX";
    // Keeps the raised DEBUG detail out of stdout; see the class Javadoc.
    static final String ENV_QUARKUS_LOG_CONSOLE_LEVEL = "QUARKUS_LOG_CONSOLE_LEVEL";

    // Environment-variable form of quarkus.log.category."<category>".level: the category is upper-cased with its dots
    // turned into single underscores, wrapped by the double-underscore quoted-segment boundaries.
    static final String ENV_QUARKUS_LOG_CATEGORY_SOLVER_LEVEL =
            "QUARKUS_LOG_CATEGORY__" + DEBUG_CATEGORY.toUpperCase(Locale.ROOT).replace('.', '_') + "__LEVEL";

    static final String LOG_FILE_PATH = EnvironmentVars.DEFAULT_EXECUTION_PROFILE_DIR + "/" + ID + "/" + LOG_FILE_NAME;

    @Override
    public String id() {
        return ID;
    }

    @Override
    public String name() {
        return "Verbose logging";
    }

    @Override
    public String description() {
        return "Captures verbose (DEBUG) solver logs into a size-bounded, rotating file that is uploaded as a run artifact.";
    }

    @Override
    public Map<String, String> toEnvironment(Map<String, String> options) {
        return Map.of(
                ENV_QUARKUS_LOG_FILE_ENABLE, "true",
                ENV_QUARKUS_LOG_FILE_PATH, LOG_FILE_PATH,
                ENV_QUARKUS_LOG_FILE_ROTATION_MAX_FILE_SIZE, MAX_FILE_SIZE,
                ENV_QUARKUS_LOG_FILE_ROTATION_MAX_BACKUP_INDEX, MAX_BACKUP_INDEX,
                ENV_QUARKUS_LOG_CATEGORY_SOLVER_LEVEL, "DEBUG",
                ENV_QUARKUS_LOG_CONSOLE_LEVEL, "INFO");
    }
}
