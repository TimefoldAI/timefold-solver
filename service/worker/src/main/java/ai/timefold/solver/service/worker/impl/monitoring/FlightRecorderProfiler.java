package ai.timefold.solver.service.worker.impl.monitoring;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.text.ParseException;

import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.interceptor.Interceptor;

import ai.timefold.solver.service.definition.api.ModelOutput;
import ai.timefold.solver.service.definition.api.ModelPostProcessor;
import ai.timefold.solver.service.definition.api.SolverModel;
import ai.timefold.solver.service.definition.internal.platform.EnvironmentVars;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.quarkus.arc.Unremovable;
import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import jdk.jfr.Configuration;
import jdk.jfr.FlightRecorder;
import jdk.jfr.Recording;

/**
 * Records a Java Flight Recorder (JFR) profile of the run when enabled
 * ({@code timefold.platform.monitoring.jfr.enabled}, set by the {@code performance-profiling} execution profile).
 * <p>
 * The recording starts with the JDK's built-in {@code profile} settings when the worker starts, and is saved into the
 * {@value #PROFILE_DIR_NAME} subdirectory of the execution-profile artifacts directory whenever the run completes, fails or the
 * worker shuts down. A later save replaces the earlier file with a longer recording. Saving has to happen before the artifacts
 * are uploaded, so this post processor runs ahead of the others ({@link Priority}, higher goes first) and its shutdown observer
 * runs ahead of the default ones (lower goes first). The file is written elsewhere and moved into place, so an upload running
 * on another thread never picks up a half-written recording. The recording keeps running across saves, the same as
 * {@code jcmd JFR.dump}, and is only closed when the worker is destroyed.
 * <p>
 * A JVM without JFR, such as a native image built without it, skips the recording with a warning.
 */
@Priority(Interceptor.Priority.PLATFORM_AFTER)
@ApplicationScoped
@Unremovable
public class FlightRecorderProfiler implements ModelPostProcessor {

    static final String PROFILE_DIR_NAME = "performance-profiling";
    static final String RECORDING_FILE_NAME = "solver-profile.jfr";
    static final String SETTINGS_NAME = "profile";

    private static final Logger LOGGER = LoggerFactory.getLogger(FlightRecorderProfiler.class);

    private final boolean enabled;
    private final Path recordingFile;
    private Recording recording;

    @Inject
    public FlightRecorderProfiler(
            @ConfigProperty(name = "timefold.platform.monitoring.jfr.enabled", defaultValue = "false") Boolean enabled,
            @ConfigProperty(name = "timefold.platform.execution-profile.dir",
                    defaultValue = EnvironmentVars.DEFAULT_EXECUTION_PROFILE_DIR) String executionProfileDir) {
        this(enabled, Path.of(executionProfileDir));
    }

    public FlightRecorderProfiler(boolean enabled, Path executionProfileDir) {
        this.enabled = enabled;
        this.recordingFile = executionProfileDir.toAbsolutePath().resolve(PROFILE_DIR_NAME).resolve(RECORDING_FILE_NAME);
    }

    public void onStart(@Observes StartupEvent event) {
        if (enabled && System.getenv(EnvironmentVars.ENV_TIMEFOLD_JOB_ID) != null) {
            start();
        }
    }

    public void onShutdown(@Observes @Priority(Interceptor.Priority.PLATFORM_BEFORE) ShutdownEvent event) {
        save();
    }

    @Override
    public void processComputed(ModelOutput modelOutput, SolverModel<?> solverModel, String id) {
        // no op: the recording already covers validation and computing the outputs, but a
        // later save on completion, failure or shutdown (which also covers compute-only runs) replaces it anyway.
    }

    @Override
    public void process(ModelOutput modelOutput, SolverModel<?> solverModel, String id) {
        save();
    }

    @Override
    public void processFailed(String id, Throwable error) {
        save();
    }

    void start() {
        if (recording != null) {
            return;
        }
        if (!FlightRecorder.isAvailable()) {
            LOGGER.warn("Performance profiling enabled but Java Flight Recorder is not available in this JVM; "
                    + "no recording will be made.");
            return;
        }
        Recording newRecording = null;
        try {
            newRecording = new Recording(Configuration.getConfiguration(SETTINGS_NAME));
            newRecording.setName("timefold-" + PROFILE_DIR_NAME);
            newRecording.setToDisk(true);
            newRecording.start();
            recording = newRecording;
            LOGGER.info("Performance profiling enabled: recording with Java Flight Recorder ({} settings)", SETTINGS_NAME);
        } catch (IOException | ParseException | RuntimeException e) {
            // The recording stays open on success for the lifetime of the worker; only a failed start must release it.
            if (newRecording != null) {
                newRecording.close();
            }
            LOGGER.warn("Unable to start Java Flight Recorder recording due to {}", e.getMessage());
        }
    }

    /**
     * @return the saved recording, or null if there is no recording or it could not be saved
     */
    Path save() {
        if (recording == null) {
            return null;
        }
        Path tempFile = null;
        try {
            Files.createDirectories(recordingFile.getParent());
            tempFile = Files.createTempFile("solver-profile-", ".jfr");
            recording.dump(tempFile);
            move(tempFile, recordingFile);
            LOGGER.info("Java Flight Recorder recording written to {}", recordingFile);
            return recordingFile;
        } catch (IOException | RuntimeException e) {
            LOGGER.warn("Unable to write Java Flight Recorder recording to {} due to {}", recordingFile, e.getMessage());
            return null;
        } finally {
            if (tempFile != null) {
                try {
                    Files.deleteIfExists(tempFile);
                } catch (IOException e) {
                    LOGGER.debug("Unable to delete temporary recording {}", tempFile);
                }
            }
        }
    }

    private static void move(Path source, Path target) throws IOException {
        try {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            // Different file systems; a plain move is the best we can do.
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
