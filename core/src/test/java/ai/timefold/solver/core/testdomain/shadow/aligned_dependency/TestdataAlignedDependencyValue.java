package ai.timefold.solver.core.testdomain.shadow.aligned_dependency;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.InverseRelationShadowVariable;
import ai.timefold.solver.core.api.domain.variable.PreviousElementShadowVariable;
import ai.timefold.solver.core.api.domain.variable.ShadowSources;
import ai.timefold.solver.core.api.domain.variable.ShadowVariable;

import org.apache.commons.lang3.ObjectUtils;

/**
 * A value that waits for its dependencies to end,
 * and that starts at the same time as its aligned values.
 * <p>
 * Both {@link #dependencies} and {@link #alignedValues} are group sources,
 * but on different shadow variables:
 * {@link #readyTime} (the first variable) reads the dependencies,
 * {@link #startTime} (a later variable) reads the aligned values.
 * <p>
 * It has no {@link ai.timefold.solver.core.api.domain.variable.ShadowVariablesInconsistent} field,
 * so the solver skips every solution with a loop.
 */
@PlanningEntity
public class TestdataAlignedDependencyValue {
    String id;
    Duration duration;
    List<TestdataAlignedDependencyValue> dependencies;
    /**
     * The other values that must start at the same time as this one; empty when this value is not aligned.
     */
    List<TestdataAlignedDependencyValue> alignedValues;

    @PreviousElementShadowVariable(sourceVariableName = "values")
    TestdataAlignedDependencyValue previousValue;

    @InverseRelationShadowVariable(sourceVariableName = "values")
    TestdataAlignedDependencyEntity entity;

    @ShadowVariable(supplierName = "calculateReadyTime")
    LocalDateTime readyTime;

    @ShadowVariable(supplierName = "calculateStartTime")
    LocalDateTime startTime;

    @ShadowVariable(supplierName = "calculateEndTime")
    LocalDateTime endTime;

    public TestdataAlignedDependencyValue() {
    }

    public TestdataAlignedDependencyValue(String id, Duration duration) {
        this(id, duration, List.of());
    }

    public TestdataAlignedDependencyValue(String id, Duration duration, List<TestdataAlignedDependencyValue> dependencies) {
        this.id = id;
        this.duration = duration;
        this.dependencies = dependencies;
        this.alignedValues = List.of();
    }

    /**
     * Makes the given values start at the same time.
     */
    public static void align(TestdataAlignedDependencyValue... values) {
        var valueList = List.of(values);
        for (var value : values) {
            value.alignedValues = valueList.stream()
                    .filter(otherValue -> otherValue != value)
                    .toList();
        }
    }

    @ShadowSources({ "dependencies[].endTime", "previousValue.endTime", "entity" })
    public LocalDateTime calculateReadyTime() {
        LocalDateTime readyTime;
        if (previousValue != null) {
            readyTime = previousValue.endTime;
        } else if (entity != null) {
            readyTime = entity.startTime;
        } else {
            return null;
        }
        for (var dependency : dependencies) {
            if (dependency.endTime == null) {
                return null;
            }
            readyTime = ObjectUtils.max(readyTime, dependency.endTime);
        }
        return readyTime;
    }

    @ShadowSources({ "readyTime", "alignedValues[].readyTime" })
    public LocalDateTime calculateStartTime() {
        if (readyTime == null) {
            return null;
        }
        var startTime = readyTime;
        for (var alignedValue : alignedValues) {
            if (alignedValue.readyTime == null) {
                return null;
            }
            startTime = ObjectUtils.max(startTime, alignedValue.readyTime);
        }
        return startTime;
    }

    @ShadowSources({ "startTime" })
    public LocalDateTime calculateEndTime() {
        if (startTime == null) {
            return null;
        }
        return startTime.plus(duration);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Duration getDuration() {
        return duration;
    }

    public void setDuration(Duration duration) {
        this.duration = duration;
    }

    public List<TestdataAlignedDependencyValue> getDependencies() {
        return dependencies;
    }

    public void setDependencies(List<TestdataAlignedDependencyValue> dependencies) {
        this.dependencies = dependencies;
    }

    public List<TestdataAlignedDependencyValue> getAlignedValues() {
        return alignedValues;
    }

    public void setAlignedValues(List<TestdataAlignedDependencyValue> alignedValues) {
        this.alignedValues = alignedValues;
    }

    public TestdataAlignedDependencyValue getPreviousValue() {
        return previousValue;
    }

    public void setPreviousValue(TestdataAlignedDependencyValue previousValue) {
        this.previousValue = previousValue;
    }

    public TestdataAlignedDependencyEntity getEntity() {
        return entity;
    }

    public void setEntity(TestdataAlignedDependencyEntity entity) {
        this.entity = entity;
    }

    public LocalDateTime getReadyTime() {
        return readyTime;
    }

    public void setReadyTime(LocalDateTime readyTime) {
        this.readyTime = readyTime;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    @Override
    public String toString() {
        return id + "{" +
                "startTime=" + startTime +
                ", endTime=" + endTime +
                '}';
    }
}
