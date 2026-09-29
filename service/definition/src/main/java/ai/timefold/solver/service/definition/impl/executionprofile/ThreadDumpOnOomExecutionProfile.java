package ai.timefold.solver.service.definition.impl.executionprofile;

import java.util.Map;

import ai.timefold.solver.service.definition.internal.executionprofile.ExecutionProfile;
import ai.timefold.solver.service.definition.internal.platform.EnvironmentVars;

/**
 * Writes a thread dump when the solver JVM fails with a {@link OutOfMemoryError}.
 * <p>
 * The profile supplies an {@code -XX:OnOutOfMemoryError} hook that sends the JVM a {@code SIGQUIT}, plus VM output logging
 * that captures the resulting thread dump into the run's execution artifacts, through {@code JAVA_TOOL_OPTIONS}, together with
 * a deliberately reduced {@code -Xmx}. The heap ceiling is derived from the
 * pod's memory allocation, supplied as the required {@code memoryLimitMb} run option, and is
 * {@value #HEAP_PERCENTAGE}% of it.
 * <p>
 * The platform detects out-of-memory failures by the container's {@code OOMKilled} termination
 * reason, a kernel SIGKILL issued once the container exceeds its cgroup memory limit. {@code -XX:OnOutOfMemoryError} only
 * fires on an in-JVM {@code java.lang.OutOfMemoryError}; a SIGKILL gives the JVM no chance to run any error handling.
 * Capping the heap below the container limit makes it substantially more likely that the JVM hits its own ceiling, and
 * gets to write the dump, before the kernel steps in.
 */
public final class ThreadDumpOnOomExecutionProfile implements ExecutionProfile {

    static final String PARAMETER_MEMORY_LIMIT_MB = "memoryLimitMb";

    static final String ENV_JAVA_TOOL_OPTIONS = "JAVA_TOOL_OPTIONS";

    static final String DUMP_DIRECTORY = "${" + EnvironmentVars.ENV_TIMEFOLD_EXECUTION_PROFILE_DIR + ":-"
            + EnvironmentVars.DEFAULT_EXECUTION_PROFILE_DIR + "}";

    /** Share of the pod memory allocation given to the heap; the remainder is headroom below the container limit. */
    static final int HEAP_PERCENTAGE = 75;

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
        return "Writes a thread dump to the run's execution artifacts when the JVM runs out of memory, using a reduced heap "
                + "ceiling so the JVM fails before the container is OOMKilled. "
                + "The pod memory allocation must be supplied in MiB as the '" + PARAMETER_MEMORY_LIMIT_MB + "' run option.";
    }

    @Override
    public Map<String, String> toEnvironment(Map<String, String> options) {
        String value = options == null ? null : options.get(PARAMETER_MEMORY_LIMIT_MB);
        if (value == null) {
            throw new IllegalArgumentException(
                    "Execution profile '" + id() + "' requires the '" + PARAMETER_MEMORY_LIMIT_MB
                            + "' option to be supplied.");
        }
        long memoryLimitMb;
        try {
            memoryLimitMb = Long.parseLong(value);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                    "Execution profile '" + id() + "' requires option '" + PARAMETER_MEMORY_LIMIT_MB
                            + "' to be a long, but was: " + value);
        }
        long heapMb = memoryLimitMb * HEAP_PERCENTAGE / 100;
        if (heapMb < 1) {
            throw new IllegalArgumentException(
                    "Execution profile '" + id() + "' requires option '" + PARAMETER_MEMORY_LIMIT_MB
                            + "' to be a positive number of MiB, but was: " + value);
        }
        return Map.of(ENV_JAVA_TOOL_OPTIONS, "-Xmx" + heapMb + "m " + VM_LOG_FLAGS + " " + ON_OOM_FLAG);
    }
}
