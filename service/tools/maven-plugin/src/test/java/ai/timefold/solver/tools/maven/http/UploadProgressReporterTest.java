package ai.timefold.solver.tools.maven.http;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;

import ai.timefold.solver.tools.maven.utils.InMemoryMojoLog;
import ai.timefold.solver.tools.maven.utils.InMemoryMojoLog.Level;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * The clock and the byte counter are both injected, so every throttling rule is asserted without sleeping and none of
 * these tests depend on wall clock time.
 */
class UploadProgressReporterTest {

    private static final long ONE_KIB = 1024L;
    private static final long ONE_MIB = 1024L * 1024L;

    private final InMemoryMojoLog log = new InMemoryMojoLog();
    private final AtomicLong nanos = new AtomicLong();
    private final AtomicLong transferred = new AtomicLong();

    private UploadProgressReporter createReporter(long contentLength) {
        UploadProgressSettings settings =
                new UploadProgressSettings(Duration.ofSeconds(5), Duration.ofSeconds(15), 5, ONE_MIB);
        return new UploadProgressReporter(log, settings, contentLength, transferred::get, nanos::get);
    }

    private void advance(Duration duration) {
        nanos.addAndGet(duration.toNanos());
    }

    @Test
    void reportsUploadPercentageThrottledByPercentageDelta() {
        UploadProgressReporter reporter = createReporter(10 * ONE_MIB);

        advance(Duration.ofSeconds(5));
        transferred.set(ONE_MIB);
        reporter.heartbeat();
        assertThat(log.messages())
                .containsExactly("Uploading model descriptor archive: 10% (1.0 MiB of 10.0 MiB, 5 s elapsed)");

        // 12% is only 2 percentage points on from the last line, and maximumSilence has not elapsed either
        advance(Duration.ofSeconds(5));
        transferred.set(ONE_MIB + 200 * ONE_KIB);
        reporter.heartbeat();
        assertThat(log.messages()).hasSize(1);

        advance(Duration.ofSeconds(5));
        transferred.set(2 * ONE_MIB);
        reporter.heartbeat();
        assertThat(log.messages()).hasSize(2)
                .last().isEqualTo("Uploading model descriptor archive: 20% (2.0 MiB of 10.0 MiB, 15 s elapsed)");
    }

    @Test
    void breaksTheSilenceWhenTheUploadBarelyMoves() {
        UploadProgressReporter reporter = createReporter(10 * ONE_MIB);
        transferred.set(1);

        // the first heartbeat always reports, so that feedback starts after a single interval
        advance(Duration.ofSeconds(5));
        reporter.heartbeat();
        assertThat(log.messages())
                .containsExactly("Uploading model descriptor archive: 0% (1 B of 10.0 MiB, 5 s elapsed)");

        // still 0%, and only 5 s then 10 s since the last line, so both are suppressed
        advance(Duration.ofSeconds(5));
        reporter.heartbeat();
        advance(Duration.ofSeconds(5));
        reporter.heartbeat();
        assertThat(log.messages()).hasSize(1);

        // 15 s since the last line: report anyway, so a dying connection never looks hung
        advance(Duration.ofSeconds(5));
        reporter.heartbeat();
        assertThat(log.messages()).hasSize(2)
                .last().isEqualTo("Uploading model descriptor archive: 0% (1 B of 10.0 MiB, 20 s elapsed)");
    }

    @Test
    void reportsWaitingOncePlatformHasEverythingItNeeds() {
        UploadProgressReporter reporter = createReporter(25_000);
        transferred.set(25_000);

        advance(Duration.ofSeconds(5));
        reporter.heartbeat();
        assertThat(log.messages()).containsExactly(
                "Uploaded 24.4 KiB, waiting for the Timefold Platform to process it (5 s elapsed)");

        advance(Duration.ofSeconds(5));
        reporter.heartbeat();
        assertThat(log.messages()).hasSize(1);

        advance(Duration.ofSeconds(10));
        reporter.heartbeat();
        assertThat(log.messages()).hasSize(2).last().isEqualTo(
                "Uploaded 24.4 KiB, waiting for the Timefold Platform to process it (20 s elapsed)");
    }

    @Test
    void staysCompletelyQuietWhenNothingWasReported() {
        UploadProgressReporter reporter = createReporter(25_000);
        transferred.set(25_000);

        // the common case: the request completed before the first heartbeat ever fired
        reporter.finished();

        assertThat(log.messages()).isEmpty();
    }

    @Test
    void logsAClosingLineAfterReporting() {
        UploadProgressReporter reporter = createReporter(6 * ONE_MIB);
        transferred.set(6 * ONE_MIB);

        advance(Duration.ofSeconds(5));
        reporter.heartbeat();
        reporter.finished();

        assertThat(log.messages()).hasSize(2)
                .last().isEqualTo("Model descriptor archive transfer finished: 6.0 MiB in 5 s");
        assertThat(log.contains("transfer finished", Level.INFO)).isTrue();
    }

    @ParameterizedTest
    @CsvSource({ "0, 0 B", "1023, 1023 B", "1024, 1.0 KiB", "25000, 24.4 KiB", "1048576, 1.0 MiB",
            "6291456, 6.0 MiB" })
    void formatsBytes(long bytes, String expected) {
        // Locale.ROOT is asserted implicitly: a comma decimal separator would fail here on a non-English agent
        assertThat(UploadProgressReporter.formatBytes(bytes)).isEqualTo(expected);
    }

    @ParameterizedTest
    @CsvSource({ "0, 0 s", "59, 59 s", "60, 1 m 0 s", "130, 2 m 10 s" })
    void formatsDuration(long seconds, String expected) {
        assertThat(UploadProgressReporter.formatDuration(Duration.ofSeconds(seconds))).isEqualTo(expected);
    }

}
