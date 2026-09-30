package ai.timefold.solver.service.definition.impl.executionprofile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Map;

import org.junit.jupiter.api.Test;

class ThreadDumpOnOomExecutionProfileTest {

    private static final String OOM_FLAGS =
            "-XX:+UnlockDiagnosticVMOptions -XX:+LogVMOutput -XX:LogFile=/tmp/timefold-jvm-%p.log "
                    + "-XX:OnOutOfMemoryError='mkdir -p ${AI_TIMEFOLD_EXECUTION_PROFILE_DIR:-/tmp/timefold-execution-artifacts}; "
                    + "ln -f /tmp/timefold-jvm-pid%p.log "
                    + "${AI_TIMEFOLD_EXECUTION_PROFILE_DIR:-/tmp/timefold-execution-artifacts}/thread-dump-%p.log; "
                    + "kill -3 %p'";

    private final ThreadDumpOnOomExecutionProfile profile = new ThreadDumpOnOomExecutionProfile();

    @Test
    void addsOomHook() {
        assertThat(profile.toEnvironment(Map.of()))
                .containsExactly(Map.entry(ThreadDumpOnOomExecutionProfile.ENV_JAVA_TOOL_OPTIONS, OOM_FLAGS));
    }

    @Test
    void setsMaxHeapWhenSupplied() {
        assertThat(profile.toEnvironment(Map.of(ThreadDumpOnOomExecutionProfile.PARAMETER_MAX_HEAP_MB, "512")))
                .containsExactly(Map.entry(ThreadDumpOnOomExecutionProfile.ENV_JAVA_TOOL_OPTIONS, "-Xmx512m " + OOM_FLAGS));
    }

    @Test
    void ignoresUnrelatedOptions() {
        assertThat(profile.toEnvironment(Map.of("seed", "1")))
                .isEqualTo(profile.toEnvironment(Map.of()));
    }

    @Test
    void acceptsNullOptions() {
        assertThat(profile.toEnvironment(null)).isEqualTo(profile.toEnvironment(Map.of()));
    }

    @Test
    void rejectsInvalidMaxHeap() {
        assertThatThrownBy(() -> profile.toEnvironment(Map.of(ThreadDumpOnOomExecutionProfile.PARAMETER_MAX_HEAP_MB, "lots")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> profile.toEnvironment(Map.of(ThreadDumpOnOomExecutionProfile.PARAMETER_MAX_HEAP_MB, "0")))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
