package ai.timefold.solver.service.worker.impl.monitoring;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.OptionalLong;

import ai.timefold.solver.service.definition.internal.platform.EnvironmentVars;

/**
 * Reads the process memory the memory watchdog in {@link GcOverheadMonitoring} compares against the container memory limit.
 * <p>
 * The process resident set size (RSS) is the memory the process actually uses, both the heap and everything outside it, so it
 * is the closest single figure to what gets a container {@code OOMKilled}. It also counts heap that only holds garbage not yet
 * collected, so the threshold should leave some margin for that.
 */
final class MemoryPressure {

    static final Path PROC_SELF_STATUS = Path.of("/proc/self/status");

    private MemoryPressure() {
    }

    static OptionalLong readRssBytes() {
        try {
            return parseRssBytes(Files.readAllLines(PROC_SELF_STATUS));
        } catch (IOException | RuntimeException e) {
            // Not on Linux or not readable; the RSS signal is simply unavailable.
            return OptionalLong.empty();
        }
    }

    static OptionalLong parseRssBytes(List<String> statusLines) {
        for (var line : statusLines) {
            if (line.startsWith("VmRSS:")) {
                var parts = line.substring("VmRSS:".length()).trim().split("\\s+");
                try {
                    return OptionalLong.of(Long.parseLong(parts[0]) * 1024);
                } catch (NumberFormatException e) {
                    return OptionalLong.empty();
                }
            }
        }
        return OptionalLong.empty();
    }

    static OptionalLong containerMemoryLimitBytes() {
        return parseLimitBytes(System.getenv(EnvironmentVars.K8S_INFO_MEMORY_LIMIT));
    }

    static OptionalLong parseLimitBytes(String value) {
        if (value == null || value.isBlank()) {
            return OptionalLong.empty();
        }
        try {
            var limit = Long.parseLong(value.trim());
            return limit > 0 ? OptionalLong.of(limit) : OptionalLong.empty();
        } catch (NumberFormatException e) {
            return OptionalLong.empty();
        }
    }

    /**
     * @return a description of the memory usage if it reached the threshold, or null otherwise
     */
    static String exceededReason(long rss, long containerLimit, int thresholdPct) {
        if (rss * 100 >= containerLimit * thresholdPct) {
            return "process memory " + toMiB(rss) + " MiB of container limit "
                    + toMiB(containerLimit) + " MiB";
        }
        return null;
    }

    private static long toMiB(long bytes) {
        return bytes / (1024 * 1024);
    }
}
