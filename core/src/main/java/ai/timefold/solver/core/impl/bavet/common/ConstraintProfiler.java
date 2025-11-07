package ai.timefold.solver.core.impl.bavet.common;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

import ai.timefold.solver.core.config.score.director.ConstraintProfilingMode;
import ai.timefold.solver.core.impl.util.MutableLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.micrometer.core.instrument.Clock;

public final class ConstraintProfiler {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final Clock clock;
    private final ConstraintProfilingMode profilingMode;
    private final Map<ConstraintNodeProfileId, MutableLong> profileIdToRetractRuntime;
    private final Map<ConstraintNodeProfileId, MutableLong> profileIdToUpdateRuntime;
    private final Map<ConstraintNodeProfileId, MutableLong> profileIdToInsertRuntime;

    public ConstraintProfiler(ConstraintProfilingMode profilingMode) {
        this(Clock.SYSTEM, profilingMode);
    }

    public ConstraintProfiler(Clock clock, ConstraintProfilingMode profilingMode) {
        this.clock = clock;
        this.profilingMode = profilingMode;
        this.profileIdToRetractRuntime = new LinkedHashMap<>();
        this.profileIdToUpdateRuntime = new LinkedHashMap<>();
        this.profileIdToInsertRuntime = new LinkedHashMap<>();
    }

    public void register(ConstraintNodeProfileId profileId) {
        // When phase changes, the node network is recalculated,
        // but the profiler is reused
        profileIdToRetractRuntime.putIfAbsent(profileId, new MutableLong());
        profileIdToUpdateRuntime.putIfAbsent(profileId, new MutableLong());
        profileIdToInsertRuntime.putIfAbsent(profileId, new MutableLong());
    }

    public void measure(ConstraintNodeProfileId profileId, Operation operation, Runnable measurable) {
        var start = clock.monotonicTime();
        measurable.run();
        var end = clock.monotonicTime();
        var duration = end - start;
        switch (operation) {
            case RETRACT -> profileIdToRetractRuntime.get(profileId).add(duration);
            case UPDATE -> profileIdToUpdateRuntime.get(profileId).add(duration);
            case INSERT -> profileIdToInsertRuntime.get(profileId).add(duration);
        }
    }

    String getSummary() {
        var summary = new StringBuilder("Constraint Profiling Summary");
        Map<ConstraintNodeLocation.LocationKeyAndDisplay, MutableLong> methodIdToTotalRuntime = new LinkedHashMap<>();
        var totalDuration = 0L;
        totalDuration += getTotalDuration(methodIdToTotalRuntime, profileIdToRetractRuntime);
        totalDuration += getTotalDuration(methodIdToTotalRuntime, profileIdToUpdateRuntime);
        totalDuration += getTotalDuration(methodIdToTotalRuntime, profileIdToInsertRuntime);

        long finalTotalDuration = totalDuration;
        methodIdToTotalRuntime.entrySet()
                .stream()
                .sorted((Comparator<Map.Entry<ConstraintNodeLocation.LocationKeyAndDisplay, MutableLong>>) Comparator
                        .comparing(entry -> (Comparable) ((Map.Entry) entry).getValue())
                        .thenComparing(entry -> ((Map.Entry) entry).getKey())
                        .reversed())
                .forEach(entry -> {
                    var percentage = entry.getValue().doubleValue() / finalTotalDuration;
                    summary.append('\n')
                            .append(entry.getKey().display())
                            .append(' ')
                            .append(String.format("%.2f", percentage * 100))
                            .append('%');
                });

        return summary.toString();
    }

    public void summarize() {
        logger.info(getSummary());

    }

    private long getTotalDuration(Map<ConstraintNodeLocation.LocationKeyAndDisplay, MutableLong> methodIdToTotalRuntime,
            Map<ConstraintNodeProfileId, MutableLong> profileIdToRuntime) {
        var totalDuration = 0L;
        Function<ConstraintNodeLocation, ConstraintNodeLocation.LocationKeyAndDisplay> profileIdToKey = switch (profilingMode) {
            case BY_METHOD -> ConstraintNodeLocation::getMethodId;
            case BY_LINE -> ConstraintNodeLocation::getLineId;
            case NONE -> throw new IllegalStateException("Impossible state: profiling is disabled");
        };
        for (var entry : profileIdToRuntime.entrySet()) {
            var duration = entry.getValue().longValue();
            for (var location : entry.getKey().locationSet()) {
                methodIdToTotalRuntime.computeIfAbsent(profileIdToKey.apply(location), k -> new MutableLong())
                        .add(duration);
            }
            totalDuration += duration;
        }
        return totalDuration;
    }

    public enum Operation {
        RETRACT,
        UPDATE,
        INSERT
    }
}
