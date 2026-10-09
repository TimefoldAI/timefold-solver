package ai.timefold.solver.service.worker.impl.monitoring;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.OptionalLong;

import org.junit.jupiter.api.Test;

class MemoryPressureTest {

    private static final long MIB = 1024 * 1024;

    @Test
    void parsesRssFromProcStatus() {
        List<String> status = List.of("Name:\tjava", "VmPeak:\t 9000 kB", "VmRSS:\t  2048 kB", "Threads:\t42");
        assertThat(MemoryPressure.parseRssBytes(status)).hasValue(2048 * 1024);
    }

    @Test
    void rssUnavailableWithoutVmRssLine() {
        assertThat(MemoryPressure.parseRssBytes(List.of("Name:\tjava"))).isEmpty();
    }

    @Test
    void parsesLimit() {
        assertThat(MemoryPressure.parseLimitBytes("1073741824\n")).hasValue(1073741824);
    }

    @Test
    void treatsUnlimitedAsNoLimit() {
        assertThat(MemoryPressure.parseLimitBytes("max\n")).isEmpty(); // cgroup v2
        assertThat(MemoryPressure.parseLimitBytes("9223372036854771712")).isEmpty(); // cgroup v1
        assertThat(MemoryPressure.parseLimitBytes(null)).isEmpty();
        assertThat(MemoryPressure.parseLimitBytes("")).isEmpty();
    }

    @Test
    void belowThreshold() {
        assertThat(MemoryPressure.exceededReason(OptionalLong.of(899 * MIB), OptionalLong.of(1000 * MIB), 90)).isNull();
    }

    @Test
    void processMemoryReachesThreshold() {
        assertThat(MemoryPressure.exceededReason(OptionalLong.of(900 * MIB), OptionalLong.of(1000 * MIB), 90))
                .isEqualTo("process memory 900 MiB of container limit 1000 MiB");
    }

    @Test
    void ignoredWithoutContainerLimit() {
        assertThat(MemoryPressure.exceededReason(OptionalLong.of(5000 * MIB), OptionalLong.empty(), 90)).isNull();
    }

    @Test
    void ignoredWithoutRss() {
        assertThat(MemoryPressure.exceededReason(OptionalLong.empty(), OptionalLong.of(1000 * MIB), 90)).isNull();
    }
}
