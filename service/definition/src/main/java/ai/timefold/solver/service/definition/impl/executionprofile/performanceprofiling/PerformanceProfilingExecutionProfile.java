package ai.timefold.solver.service.definition.impl.executionprofile.performanceprofiling;

import java.util.Map;

import ai.timefold.solver.service.definition.internal.executionprofile.ExecutionProfile;

/**
 * Records a Java Flight Recorder (JFR) profile of the run and collects it as a run artifact.
 * <p>
 * The profile enables the solver worker's flight recorder, which starts a recording with the JDK's built-in {@code profile}
 * settings when the worker starts and saves it into this profile's own subdirectory of the execution-profile artifacts
 * directory when the run completes, fails or shuts down, just before the artifacts are uploaded. JFR is part of the JVM, so
 * nothing has to be added to the image or attached at runtime, which the worker pod's hardened security context would not
 * allow anyway.
 * <p>
 * The recording is started and saved from code rather than with {@code -XX:StartFlightRecording}: the JVM only writes such a
 * recording when it exits, which is after the artifacts have been uploaded. A run whose container is {@code OOMKilled} gets no
 * recording, and native images, which are built without JFR, skip the recording with a warning.
 */
public final class PerformanceProfilingExecutionProfile implements ExecutionProfile {

    static final String ID = "performance-profiling";

    /** Environment-variable form of {@code timefold.platform.monitoring.jfr.enabled}. */
    static final String ENV_FLIGHT_RECORDER_ENABLED = "TIMEFOLD_PLATFORM_MONITORING_JFR_ENABLED";

    @Override
    public String id() {
        return ID;
    }

    @Override
    public String name() {
        return "Performance profiling";
    }

    @Override
    public String description() {
        return "Records a Java Flight Recorder profile of the run (CPU, allocations, locks and garbage collection) and adds "
                + "it to the run's execution artifacts.";
    }

    @Override
    public Map<String, String> toEnvironment(Map<String, String> options) {
        return Map.of(ENV_FLIGHT_RECORDER_ENABLED, "true");
    }
}
