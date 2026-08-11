package ai.timefold.solver.tools.maven.http;

import java.time.Duration;
import java.util.Locale;
import java.util.Objects;
import java.util.function.LongSupplier;

import org.apache.maven.plugin.logging.Log;

/**
 * Turns the byte counter of a {@link CountingBodyPublisher} into a small number of throttled {@code [INFO]} lines.
 * Maven's {@link Log} prefixes every line with {@code [INFO]} and cannot do in-place carriage return updates (and a
 * bare carriage return is garbage in CI logs), so progress is reported as discrete, deliberately rare lines rather
 * than as a progress bar.
 * <p>
 * Not thread safe by design: {@link #heartbeat()} and {@link #finished()} are only ever called from the Maven thread
 * executing the mojo, so the throttling state needs no synchronization and nothing is ever logged from an HTTP client
 * thread. Only the byte counter itself crosses threads, and that is an
 * {@link java.util.concurrent.atomic.AtomicLong AtomicLong} inside {@link CountingBodyPublisher}.
 */
public final class UploadProgressReporter {

    private final Log log;
    private final UploadProgressSettings settings;
    private final long contentLength;
    private final LongSupplier transferredBytesSupplier;
    private final LongSupplier nanoTimeSupplier;

    private final long startNanos;
    private long lastReportNanos;
    private int lastReportedPercentage;
    private boolean reportedAnything;

    public UploadProgressReporter(Log log, UploadProgressSettings settings, long contentLength,
            LongSupplier transferredBytesSupplier) {
        this(log, settings, contentLength, transferredBytesSupplier, System::nanoTime);
    }

    /**
     * Visible for testing: an injected clock makes the throttling rules verifiable without sleeping.
     */
    UploadProgressReporter(Log log, UploadProgressSettings settings, long contentLength,
            LongSupplier transferredBytesSupplier, LongSupplier nanoTimeSupplier) {
        this.log = Objects.requireNonNull(log, "log");
        this.settings = Objects.requireNonNull(settings, "settings");
        this.contentLength = contentLength;
        this.transferredBytesSupplier = Objects.requireNonNull(transferredBytesSupplier, "transferredBytesSupplier");
        this.nanoTimeSupplier = Objects.requireNonNull(nanoTimeSupplier, "nanoTimeSupplier");
        this.startNanos = this.nanoTimeSupplier.getAsLong();
        this.lastReportNanos = this.startNanos;
    }

    public Duration getHeartbeatInterval() {
        return settings.heartbeatInterval();
    }

    /**
     * Called from the mojo thread whenever the in-flight request did not complete within the heartbeat interval.
     * Emits at most one line per call, and only when the throttling rules allow it.
     */
    public void heartbeat() {
        long now = nanoTimeSupplier.getAsLong();
        long transferred = transferredBytesSupplier.getAsLong();
        if (contentLength > 0 && transferred < contentLength) {
            int percentage = (int) (transferred * 100 / contentLength);
            if (percentage - lastReportedPercentage < settings.minimumPercentageDelta() && !isSilentForTooLong(now)) {
                return;
            }
            lastReportedPercentage = percentage;
            report(now, String.format("Uploading model descriptor archive: %d%% (%s of %s, %s elapsed)", percentage,
                    formatBytes(transferred), formatBytes(contentLength), formatElapsed(now)));
        } else if (isSilentForTooLong(now)) {
            report(now, String.format("Uploaded %s, waiting for the Timefold Platform to process it (%s elapsed)",
                    formatBytes(transferred), formatElapsed(now)));
        }
    }

    /**
     * Called from the mojo thread once the request completed, successfully or not.
     */
    public void finished() {
        if (!reportedAnything) {
            // the common case: the request was faster than a single heartbeat, so stay completely quiet
            return;
        }
        long now = nanoTimeSupplier.getAsLong();
        log.info(String.format("Model descriptor archive transfer finished: %s in %s",
                formatBytes(transferredBytesSupplier.getAsLong()), formatElapsed(now)));
    }

    private boolean isSilentForTooLong(long now) {
        // the very first heartbeat always reports, so that feedback starts after a single interval
        return !reportedAnything || now - lastReportNanos >= settings.maximumSilence().toNanos();
    }

    private void report(long now, String message) {
        lastReportNanos = now;
        reportedAnything = true;
        log.info(message);
    }

    private String formatElapsed(long now) {
        return formatDuration(Duration.ofNanos(now - startNanos));
    }

    public static String formatBytes(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        } else if (bytes < 1024 * 1024) {
            // Locale.ROOT: a comma decimal separator would break log assertions on a non-English build agent
            return String.format(Locale.ROOT, "%.1f KiB", bytes / 1024.0);
        }
        return String.format(Locale.ROOT, "%.1f MiB", bytes / (1024.0 * 1024.0));
    }

    public static String formatDuration(Duration duration) {
        long seconds = Math.max(0, duration.toSeconds());
        if (seconds < 60) {
            return seconds + " s";
        }
        return (seconds / 60) + " m " + (seconds % 60) + " s";
    }

}
