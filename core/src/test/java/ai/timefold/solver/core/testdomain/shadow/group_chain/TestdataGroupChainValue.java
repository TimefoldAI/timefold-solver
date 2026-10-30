package ai.timefold.solver.core.testdomain.shadow.group_chain;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.InverseRelationShadowVariable;
import ai.timefold.solver.core.api.domain.variable.PreviousElementShadowVariable;
import ai.timefold.solver.core.api.domain.variable.ShadowSources;
import ai.timefold.solver.core.api.domain.variable.ShadowVariable;

import org.apache.commons.lang3.ObjectUtils;

/**
 * A value with a chain of four groups of declarative shadow variables, each after a group source:
 * {@link #readyTime} (dependencies), {@link #startTime} and {@link #endTime} (aligned values),
 * {@link #handoverTime} (partners) and {@link #reportTime} (reviewers).
 * The handover time reads only the end time; the report time also reads the ready time.
 */
@PlanningEntity
public class TestdataGroupChainValue {
    String id;
    Duration duration;
    List<TestdataGroupChainValue> dependencies;
    /**
     * The other values that must start at the same time as this one.
     */
    List<TestdataGroupChainValue> alignedValues;
    /**
     * The values that must end before this one hands over.
     */
    List<TestdataGroupChainValue> partners;
    /**
     * The values that must end before this one reports.
     */
    List<TestdataGroupChainValue> reviewers;

    @PreviousElementShadowVariable(sourceVariableName = "values")
    TestdataGroupChainValue previousValue;

    @InverseRelationShadowVariable(sourceVariableName = "values")
    TestdataGroupChainEntity entity;

    @ShadowVariable(supplierName = "calculateReadyTime")
    OffsetDateTime readyTime;

    @ShadowVariable(supplierName = "calculateStartTime")
    OffsetDateTime startTime;

    @ShadowVariable(supplierName = "calculateEndTime")
    OffsetDateTime endTime;

    @ShadowVariable(supplierName = "calculateHandoverTime")
    OffsetDateTime handoverTime;

    @ShadowVariable(supplierName = "calculateReportTime")
    OffsetDateTime reportTime;

    public TestdataGroupChainValue() {
    }

    public TestdataGroupChainValue(String id, Duration duration, List<TestdataGroupChainValue> dependencies) {
        this.id = id;
        this.duration = duration;
        this.dependencies = dependencies;
        this.alignedValues = List.of();
        this.partners = List.of();
        this.reviewers = List.of();
    }

    /**
     * Makes the given values start at the same time.
     */
    public static void align(TestdataGroupChainValue... values) {
        var valueList = List.of(values);
        for (var value : values) {
            value.alignedValues = valueList.stream()
                    .filter(otherValue -> otherValue != value)
                    .toList();
        }
    }

    @ShadowSources({ "dependencies[].endTime", "previousValue.endTime", "entity" })
    public OffsetDateTime calculateReadyTime() {
        OffsetDateTime calculatedReadyTime;
        if (previousValue != null) {
            calculatedReadyTime = previousValue.endTime;
        } else if (entity != null) {
            calculatedReadyTime = entity.startTime;
        } else {
            return null;
        }
        return latestEndTime(calculatedReadyTime, dependencies);
    }

    @ShadowSources({ "readyTime", "alignedValues[].readyTime" })
    public OffsetDateTime calculateStartTime() {
        if (readyTime == null) {
            return null;
        }
        var calculatedStartTime = readyTime;
        for (var alignedValue : alignedValues) {
            if (alignedValue.readyTime == null) {
                return null;
            }
            calculatedStartTime = ObjectUtils.max(calculatedStartTime, alignedValue.readyTime);
        }
        return calculatedStartTime;
    }

    @ShadowSources({ "startTime" })
    public OffsetDateTime calculateEndTime() {
        if (startTime == null) {
            return null;
        }
        return startTime.plus(duration);
    }

    @ShadowSources({ "endTime", "partners[].endTime" })
    public OffsetDateTime calculateHandoverTime() {
        if (endTime == null) {
            return null;
        }
        return latestEndTime(endTime, partners);
    }

    /**
     * The handover time, delayed by the time this value waited between its ready time and its start time.
     */
    @ShadowSources({ "readyTime", "startTime", "handoverTime", "reviewers[].endTime" })
    public OffsetDateTime calculateReportTime() {
        if (readyTime == null || startTime == null || handoverTime == null) {
            return null;
        }
        return latestEndTime(handoverTime.plus(Duration.between(readyTime, startTime)), reviewers);
    }

    private static OffsetDateTime latestEndTime(OffsetDateTime time, List<TestdataGroupChainValue> values) {
        var latestTime = time;
        for (var value : values) {
            if (value.endTime == null) {
                return null;
            }
            latestTime = ObjectUtils.max(latestTime, value.endTime);
        }
        return latestTime;
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

    public List<TestdataGroupChainValue> getDependencies() {
        return dependencies;
    }

    public void setDependencies(List<TestdataGroupChainValue> dependencies) {
        this.dependencies = dependencies;
    }

    public List<TestdataGroupChainValue> getAlignedValues() {
        return alignedValues;
    }

    public void setAlignedValues(List<TestdataGroupChainValue> alignedValues) {
        this.alignedValues = alignedValues;
    }

    public List<TestdataGroupChainValue> getPartners() {
        return partners;
    }

    public void setPartners(List<TestdataGroupChainValue> partners) {
        this.partners = partners;
    }

    public List<TestdataGroupChainValue> getReviewers() {
        return reviewers;
    }

    public void setReviewers(List<TestdataGroupChainValue> reviewers) {
        this.reviewers = reviewers;
    }

    public TestdataGroupChainValue getPreviousValue() {
        return previousValue;
    }

    public void setPreviousValue(TestdataGroupChainValue previousValue) {
        this.previousValue = previousValue;
    }

    public TestdataGroupChainEntity getEntity() {
        return entity;
    }

    public void setEntity(TestdataGroupChainEntity entity) {
        this.entity = entity;
    }

    public OffsetDateTime getReadyTime() {
        return readyTime;
    }

    public void setReadyTime(OffsetDateTime readyTime) {
        this.readyTime = readyTime;
    }

    public OffsetDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(OffsetDateTime startTime) {
        this.startTime = startTime;
    }

    public OffsetDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(OffsetDateTime endTime) {
        this.endTime = endTime;
    }

    public OffsetDateTime getHandoverTime() {
        return handoverTime;
    }

    public void setHandoverTime(OffsetDateTime handoverTime) {
        this.handoverTime = handoverTime;
    }

    public OffsetDateTime getReportTime() {
        return reportTime;
    }

    public void setReportTime(OffsetDateTime reportTime) {
        this.reportTime = reportTime;
    }

    @Override
    public String toString() {
        return id + "{" +
                "startTime=" + startTime +
                ", endTime=" + endTime +
                '}';
    }
}
