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
 * kernel SIGKILL, which gives the JVM no chance to run any error handling, so that case is not covered. The optional
 * {@code maxHeapMb} run option sets {@code -Xmx}, which also lowers the default direct memory limit; a heap well below the
 * container limit makes the JVM run out of memory before the container does. Without it the image's default heap applies.
 */
public final class ThreadDumpOnOomExecutionProfile implements ExecutionProfile {

    static final String PARAMETER_MAX_HEAP_MB = "maxHeapMb";

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
        return "Writes a thread dump to the run's execution artifacts when the JVM runs out of memory. "
                + "Optionally, the maximum heap size in MiB can be supplied as the '" + PARAMETER_MAX_HEAP_MB + "' run option.";
    }

    @Override
    public Map<String, String> toEnvironment(Map<String, String> options) {
        String flags = VM_LOG_FLAGS + " " + ON_OOM_FLAG;
        String maxHeapMb = options == null ? null : options.get(PARAMETER_MAX_HEAP_MB);
        if (maxHeapMb == null) {
            return Map.of(ENV_JAVA_TOOL_OPTIONS, flags);
        }
        long heapMb;
        try {
            heapMb = Long.parseLong(maxHeapMb);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                    "Execution profile '" + id() + "' requires option '" + PARAMETER_MAX_HEAP_MB + "' to be a long, but was: "
                            + maxHeapMb);
        }
        if (heapMb < 1) {
            throw new IllegalArgumentException(
                    "Execution profile '" + id() + "' requires option '" + PARAMETER_MAX_HEAP_MB
                            + "' to be a positive number of MiB, but was: " + maxHeapMb);
        }
        return Map.of(ENV_JAVA_TOOL_OPTIONS, "-Xmx" + heapMb + "m " + flags);
    }
}
