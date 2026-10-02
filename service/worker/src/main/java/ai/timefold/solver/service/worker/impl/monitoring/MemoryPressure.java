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
    static final Path CGROUP_V2_MEMORY_MAX = Path.of("/sys/fs/cgroup/memory.max");
    static final Path CGROUP_V1_MEMORY_LIMIT = Path.of("/sys/fs/cgroup/memory/memory.limit_in_bytes");

    /** cgroup v1 reports an unlimited container as a value close to {@link Long#MAX_VALUE}. */
    private static final long UNLIMITED_THRESHOLD = 1L << 60;

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
        for (String line : statusLines) {
            if (line.startsWith("VmRSS:")) {
                String[] parts = line.substring("VmRSS:".length()).trim().split("\\s+");
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
        OptionalLong fromEnv = parseLimitBytes(System.getenv(EnvironmentVars.K8S_INFO_MEMORY_LIMIT));
        if (fromEnv.isPresent()) {
            return fromEnv;
        }
        for (Path path : List.of(CGROUP_V2_MEMORY_MAX, CGROUP_V1_MEMORY_LIMIT)) {
            try {
                OptionalLong limit = parseLimitBytes(Files.readString(path));
                if (limit.isPresent()) {
                    return limit;
                }
            } catch (IOException | RuntimeException e) {
                // Try the next location.
            }
        }
        return OptionalLong.empty();
    }

    static OptionalLong parseLimitBytes(String value) {
        if (value == null || value.isBlank()) {
            return OptionalLong.empty();
        }
        try {
            long limit = Long.parseLong(value.trim());
            return limit > 0 && limit < UNLIMITED_THRESHOLD ? OptionalLong.of(limit) : OptionalLong.empty();
        } catch (NumberFormatException e) {
            // cgroup v2 reports an unlimited container as "max".
            return OptionalLong.empty();
        }
    }

    /**
     * @return a description of the memory usage if it reached the threshold, or null otherwise
     */
    static String exceededReason(OptionalLong rss, OptionalLong containerLimit, int thresholdPct) {
        if (rss.isPresent() && containerLimit.isPresent()
                && rss.getAsLong() * 100 >= containerLimit.getAsLong() * thresholdPct) {
            return "process memory " + toMiB(rss.getAsLong()) + " MiB of container limit "
                    + toMiB(containerLimit.getAsLong()) + " MiB";
        }
        return null;
    }

    private static long toMiB(long bytes) {
        return bytes / (1024 * 1024);
    }
}
