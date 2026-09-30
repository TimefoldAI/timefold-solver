package ai.timefold.solver.service.definition.impl.executionprofile;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.Test;

class ThreadDumpOnOomExecutionProfileTest {

    private final ThreadDumpOnOomExecutionProfile profile = new ThreadDumpOnOomExecutionProfile();

    @Test
    void addsOomHook() {
        assertThat(profile.toEnvironment(Map.of()))
                .containsExactly(Map.entry(ThreadDumpOnOomExecutionProfile.ENV_JAVA_TOOL_OPTIONS,
                        "-XX:+UnlockDiagnosticVMOptions -XX:+LogVMOutput -XX:LogFile=/tmp/timefold-jvm-%p.log "
                                + "-XX:OnOutOfMemoryError='mkdir -p ${AI_TIMEFOLD_EXECUTION_PROFILE_DIR:-/tmp/timefold-execution-artifacts}; "
                                + "ln -f /tmp/timefold-jvm-pid%p.log "
                                + "${AI_TIMEFOLD_EXECUTION_PROFILE_DIR:-/tmp/timefold-execution-artifacts}/thread-dump-%p.log; "
                                + "kill -3 %p'"));
    }

    @Test
    void ignoresOptions() {
        // The profile takes no options; whatever is supplied is left to other profiles.
        assertThat(profile.toEnvironment(Map.of("seed", "1")))
                .isEqualTo(profile.toEnvironment(Map.of()));
    }

    @Test
    void acceptsNullOptions() {
        assertThat(profile.toEnvironment(null)).containsOnlyKeys(ThreadDumpOnOomExecutionProfile.ENV_JAVA_TOOL_OPTIONS);
    }
}
