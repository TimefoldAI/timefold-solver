package ai.timefold.solver.service.definition.impl.executionprofile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Map;

import org.junit.jupiter.api.Test;

class ThreadDumpOnOomExecutionProfileTest {

    private final ThreadDumpOnOomExecutionProfile profile = new ThreadDumpOnOomExecutionProfile();

    @Test
    void reducesHeapBelowMemoryLimitAndAddsOomHook() {
        var env = profile.toEnvironment(Map.of(ThreadDumpOnOomExecutionProfile.PARAMETER_MEMORY_LIMIT_MB, "4096"));
        assertThat(env).containsOnlyKeys(ThreadDumpOnOomExecutionProfile.ENV_JAVA_TOOL_OPTIONS);
        assertThat(env.get(ThreadDumpOnOomExecutionProfile.ENV_JAVA_TOOL_OPTIONS))
                .isEqualTo("-Xmx3072m -XX:+UnlockDiagnosticVMOptions -XX:+LogVMOutput -XX:LogFile=/tmp/timefold-jvm-%p.log "
                        + "-XX:OnOutOfMemoryError='mkdir -p ${AI_TIMEFOLD_EXECUTION_PROFILE_DIR:-/tmp/timefold-execution-artifacts}; "
                        + "ln -f /tmp/timefold-jvm-pid%p.log "
                        + "${AI_TIMEFOLD_EXECUTION_PROFILE_DIR:-/tmp/timefold-execution-artifacts}/thread-dump-%p.log; "
                        + "kill -3 %p'");
    }

    @Test
    void ignoresUnrelatedOptions() {
        assertThat(profile.toEnvironment(Map.of("seed", "1", "memoryLimitMb", "1000")))
                .containsOnlyKeys(ThreadDumpOnOomExecutionProfile.ENV_JAVA_TOOL_OPTIONS);
    }

    @Test
    void failsWhenMemoryLimitMissing() {
        assertThatThrownBy(() -> profile.toEnvironment(Map.of())).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsInvalidMemoryLimit() {
        assertThatThrownBy(() -> profile.toEnvironment(Map.of("memoryLimitMb", "lots")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> profile.toEnvironment(Map.of("memoryLimitMb", "0")))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
