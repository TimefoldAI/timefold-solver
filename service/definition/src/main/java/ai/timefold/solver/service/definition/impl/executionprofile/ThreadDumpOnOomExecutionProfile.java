package ai.timefold.solver.service.definition.impl.executionprofile;

import java.util.Map;

import ai.timefold.solver.service.definition.internal.executionprofile.ExecutionProfile;
import ai.timefold.solver.service.definition.internal.platform.EnvironmentVars;

/**
 * Writes a thread dump when the solver JVM fails with a {@link OutOfMemoryError}.
 * <p>
 * The profile supplies an {@code -XX:OnOutOfMemoryError} hook that sends the JVM a {@code SIGQUIT}, plus VM output logging
 * that captures the resulting thread dump into the run's execution artifacts, through {@code JAVA_TOOL_OPTIONS}.
 * <p>
 * The hook only fires on an in-JVM {@code java.lang.OutOfMemoryError}. A container that is {@code OOMKilled} receives a
 * kernel SIGKILL, which gives the JVM no chance to run any error handling, so that case is not covered.
 */
public final class ThreadDumpOnOomExecutionProfile implements ExecutionProfile {

    static final String ENV_JAVA_TOOL_OPTIONS = "JAVA_TOOL_OPTIONS";

    static final String DUMP_DIRECTORY = "${" + EnvironmentVars.ENV_TIMEFOLD_EXECUTION_PROFILE_DIR + ":-"
            + EnvironmentVars.DEFAULT_EXECUTION_PROFILE_DIR + "}";

    static final String VM_LOG_FLAGS = "-XX:+UnlockDiagnosticVMOptions -XX:+LogVMOutput -XX:LogFile=/tmp/timefold-jvm-%p.log";

    static final String VM_LOG_FILE = "/tmp/timefold-jvm-pid%p.log";

    static final String ON_OOM_FLAG = "-XX:OnOutOfMemoryError='mkdir -p " + DUMP_DIRECTORY + "; ln -f " + VM_LOG_FILE + " "
            + DUMP_DIRECTORY + "/thread-dump-%p.log; kill -3 %p'";

    @Override
    public String id() {
        return "thread-dump-on-oom";
    }

    @Override
    public String name() {
        return "Thread dump on OOM";
    }

    @Override
    public String description() {
        return "Writes a thread dump to the run's execution artifacts when the JVM runs out of memory.";
    }

    @Override
    public Map<String, String> toEnvironment(Map<String, String> options) {
        return Map.of(ENV_JAVA_TOOL_OPTIONS, VM_LOG_FLAGS + " " + ON_OOM_FLAG);
    }
}
