package ai.timefold.solver.tools.maven.http;

import java.time.Duration;

/**
 * Tuning of the progress feedback of an upload. {@link #defaults()} is what the deploy goal uses, tests use tighter
 * values to make the throttling observable without waiting.
 *
 * @param heartbeatInterval how often the mojo thread checks the in-flight request, which is also the hard floor
 *        between two log lines
 * @param maximumSilence after this long without a log line, the next heartbeat logs regardless of how little progress
 *        was made, so that a very slow connection never looks hung
 * @param minimumPercentageDelta how much of the upload has to have completed since the previous log line
 * @param minimumSizeToAnnounceBytes archives smaller than this are not announced up front
 */
public record UploadProgressSettings(Duration heartbeatInterval, Duration maximumSilence, int minimumPercentageDelta,
        long minimumSizeToAnnounceBytes) {

    public UploadProgressSettings {
        if (heartbeatInterval == null || heartbeatInterval.isNegative() || heartbeatInterval.isZero()) {
            // a zero timeout on Future.get() would busy loop
            throw new IllegalArgumentException(
                    "heartbeatInterval must be a positive duration but was (" + heartbeatInterval + ").");
        }
        if (maximumSilence == null || maximumSilence.isNegative()) {
            throw new IllegalArgumentException("maximumSilence must not be negative but was (" + maximumSilence + ").");
        }
        if (minimumPercentageDelta < 0 || minimumPercentageDelta > 100) {
            throw new IllegalArgumentException(
                    "minimumPercentageDelta must be between 0 and 100 but was (" + minimumPercentageDelta + ").");
        }
        if (minimumSizeToAnnounceBytes < 0) {
            throw new IllegalArgumentException(
                    "minimumSizeToAnnounceBytes must not be negative but was (" + minimumSizeToAnnounceBytes + ").");
        }
    }

    /**
     * 5 seconds is short enough that nobody concludes the build hung, and long enough that a normal deploy of a small
     * archive completes before the first heartbeat and stays entirely silent. The 15 second maximum silence caps a ten
     * minute upload at roughly 40 lines, and the 5% delta caps a fast large upload at roughly 20 lines. The 1 MiB
     * announcement threshold keeps the typical model descriptor archive of a few tens of kilobytes unannounced.
     */
    public static UploadProgressSettings defaults() {
        return new UploadProgressSettings(Duration.ofSeconds(5), Duration.ofSeconds(15), 5, 1024L * 1024L);
    }

}
