package ai.timefold.solver.service.worker.impl.monitoring;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import jdk.jfr.FlightRecorder;
import jdk.jfr.Recording;
import jdk.jfr.consumer.RecordingFile;

class FlightRecorderProfilerTest {

    @TempDir
    Path artifactsDir;

    @AfterEach
    void closeRecordings() {
        FlightRecorder.getFlightRecorder().getRecordings().stream()
                .filter(recording -> recording.getName().equals("timefold-performance-profiling"))
                .forEach(Recording::close);
    }

    @Test
    void savesRecordingIntoProfileDirectory() throws Exception {
        var profiler = new FlightRecorderProfiler(true, artifactsDir);
        profiler.start();

        var saved = profiler.save();

        assertThat(saved).isEqualTo(artifactsDir.toAbsolutePath().resolve("performance-profiling/solver-profile.jfr"))
                .isRegularFile();
        assertThat(RecordingFile.readAllEvents(saved)).isNotEmpty();
    }

    @Test
    void laterSaveReplacesEarlierRecording() {
        var profiler = new FlightRecorderProfiler(true, artifactsDir);
        profiler.start();

        var first = profiler.save();
        var second = profiler.save();

        assertThat(second).isEqualTo(first).isRegularFile();
        assertThat(artifactsDir.resolve("performance-profiling")).isDirectoryContaining(
                path -> path.getFileName().toString().equals("solver-profile.jfr"));
    }

    @Test
    void postProcessingSavesRecording() {
        var profiler = new FlightRecorderProfiler(true, artifactsDir);
        profiler.start();

        profiler.processFailed("run", new RuntimeException("failed"));

        assertThat(artifactsDir.resolve("performance-profiling/solver-profile.jfr")).isRegularFile();
    }

    @Test
    void computedOutputsDoNotSaveRecording() {
        var profiler = new FlightRecorderProfiler(true, artifactsDir);
        profiler.start();

        profiler.processComputed(null, null, "run");

        assertThat(artifactsDir.resolve("performance-profiling")).doesNotExist();
    }

    @Test
    void nothingToSaveWithoutRecording() {
        var profiler = new FlightRecorderProfiler(true, artifactsDir);

        assertThat(profiler.save()).isNull();
        assertThat(artifactsDir).isEmptyDirectory();
    }

    @Test
    void disabledOutsideOfPlatformRun() {
        // Without a job id the worker does not run a platform run, so the recording is not started even when enabled.
        var profiler = new FlightRecorderProfiler(true, artifactsDir);
        profiler.onStart(null);

        assertThat(profiler.save()).isNull();
        assertThat(Files.exists(artifactsDir.resolve("performance-profiling"))).isFalse();
    }
}
