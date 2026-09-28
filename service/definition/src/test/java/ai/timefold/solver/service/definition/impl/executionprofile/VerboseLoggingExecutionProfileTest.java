package ai.timefold.solver.service.definition.impl.executionprofile;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import ai.timefold.solver.service.definition.internal.platform.EnvironmentVars;

import org.junit.jupiter.api.Test;

class VerboseLoggingExecutionProfileTest {

    private final VerboseLoggingExecutionProfile profile = new VerboseLoggingExecutionProfile();

    @Test
    void enablesRotatingFileLoggingAtDebug() {
        assertThat(profile.toEnvironment(Map.of()))
                .containsOnly(
                        Map.entry(VerboseLoggingExecutionProfile.ENV_QUARKUS_LOG_FILE_ENABLE, "true"),
                        Map.entry(VerboseLoggingExecutionProfile.ENV_QUARKUS_LOG_FILE_PATH,
                                VerboseLoggingExecutionProfile.LOG_FILE_PATH),
                        Map.entry(VerboseLoggingExecutionProfile.ENV_QUARKUS_LOG_FILE_ROTATION_MAX_FILE_SIZE,
                                VerboseLoggingExecutionProfile.MAX_FILE_SIZE),
                        Map.entry(VerboseLoggingExecutionProfile.ENV_QUARKUS_LOG_FILE_ROTATION_MAX_BACKUP_INDEX,
                                VerboseLoggingExecutionProfile.MAX_BACKUP_INDEX),
                        Map.entry(VerboseLoggingExecutionProfile.ENV_QUARKUS_LOG_CATEGORY_SOLVER_LEVEL, "DEBUG"),
                        Map.entry(VerboseLoggingExecutionProfile.ENV_QUARKUS_LOG_CONSOLE_LEVEL, "INFO"));
    }

    @Test
    void encodesTheSolverCategoryEnvVarName() {
        // The category env-var name is derived from the dotted category, not hardcoded, so the two can never drift apart.
        assertThat(VerboseLoggingExecutionProfile.ENV_QUARKUS_LOG_CATEGORY_SOLVER_LEVEL)
                .isEqualTo("QUARKUS_LOG_CATEGORY__AI_TIMEFOLD_SOLVER__LEVEL");
    }

    @Test
    void keepsTheDebugDetailOutOfStdout() {
        // Raising the category to DEBUG alone would also surface on the console (default level ALL); pinning the console
        // handler to INFO confines the extra verbosity to the uploaded file.
        assertThat(profile.toEnvironment(Map.of()))
                .containsEntry(VerboseLoggingExecutionProfile.ENV_QUARKUS_LOG_CONSOLE_LEVEL, "INFO");
    }

    @Test
    void writesIntoItsOwnSubdirectoryOfTheArtifactsDirectory() {
        // The log must land under <artifacts-dir>/<id>/ so the post-processor bundles it, and so multiple active profiles
        // never collide on the same file.
        assertThat(profile.toEnvironment(Map.of()))
                .containsEntry(VerboseLoggingExecutionProfile.ENV_QUARKUS_LOG_FILE_PATH,
                        EnvironmentVars.DEFAULT_EXECUTION_PROFILE_DIR + "/" + profile.id() + "/"
                                + VerboseLoggingExecutionProfile.LOG_FILE_NAME);
    }

    @Test
    void ignoresRunOptions() {
        // The profile is fully static: it recognizes no options and never varies its environment.
        assertThat(profile.toEnvironment(Map.of("seed", "42", "solver", "fast")))
                .isEqualTo(profile.toEnvironment(Map.of()));
    }

    @Test
    void raisesOnlyTheSolverCategoryNotTheRootLogger() {
        Map<String, String> environment = profile.toEnvironment(Map.of());
        assertThat(environment).containsEntry(VerboseLoggingExecutionProfile.ENV_QUARKUS_LOG_CATEGORY_SOLVER_LEVEL, "DEBUG");
        // No blanket root-logger level bump.
        assertThat(environment).doesNotContainKey("QUARKUS_LOG_LEVEL");
    }
}
