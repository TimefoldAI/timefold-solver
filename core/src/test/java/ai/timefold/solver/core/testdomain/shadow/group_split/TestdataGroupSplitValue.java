package ai.timefold.solver.core.testdomain.shadow.group_split;

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
 * A value whose later shadow variables have group sources that are often empty,
 * next to sources that must keep them in separate graph nodes:
 * {@link #startTime} also reads the {@link #readyTime} of another value through a fact,
 * and {@link #handoverTime} reads its group through a planning variable.
 */
@PlanningEntity
public class TestdataGroupSplitValue {
    String id;
    Duration duration;
    List<TestdataGroupSplitValue> dependencies;
    /**
     * The other values that must start at the same time as this one.
     */
    List<TestdataGroupSplitValue> alignedValues;
    /**
     * Another value that must be ready before this one starts, or null.
     */
    TestdataGroupSplitValue peer;
    /**
     * The values whose previous value must end before this one hands over.
     */
    List<TestdataGroupSplitValue> followers;

    @PreviousElementShadowVariable(sourceVariableName = "values")
    TestdataGroupSplitValue previousValue;

    @InverseRelationShadowVariable(sourceVariableName = "values")
    TestdataGroupSplitEntity entity;

    @ShadowVariable(supplierName = "calculateReadyTime")
    LocalDateTime readyTime;

    @ShadowVariable(supplierName = "calculateStartTime")
    LocalDateTime startTime;

    @ShadowVariable(supplierName = "calculateEndTime")
    LocalDateTime endTime;

    @ShadowVariable(supplierName = "calculateHandoverTime")
    LocalDateTime handoverTime;

    public TestdataGroupSplitValue() {
    }

    public TestdataGroupSplitValue(String id, Duration duration, List<TestdataGroupSplitValue> dependencies) {
        this.id = id;
        this.duration = duration;
        this.dependencies = dependencies;
        this.alignedValues = List.of();
        this.followers = List.of();
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

    @ShadowSources({ "readyTime", "alignedValues[].readyTime", "peer.readyTime" })
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
        if (peer != null) {
            if (peer.readyTime == null) {
                return null;
            }
            startTime = ObjectUtils.max(startTime, peer.readyTime);
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

    @ShadowSources({ "endTime", "followers[].previousValue.endTime" })
    public LocalDateTime calculateHandoverTime() {
        if (endTime == null) {
            return null;
        }
        var handoverTime = endTime;
        for (var follower : followers) {
            if (follower.previousValue != null) {
                if (follower.previousValue.endTime == null) {
                    return null;
                }
                handoverTime = ObjectUtils.max(handoverTime, follower.previousValue.endTime);
            }
        }
        return handoverTime;
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

    public List<TestdataGroupSplitValue> getDependencies() {
        return dependencies;
    }

    public void setDependencies(List<TestdataGroupSplitValue> dependencies) {
        this.dependencies = dependencies;
    }

    public List<TestdataGroupSplitValue> getAlignedValues() {
        return alignedValues;
    }

    public void setAlignedValues(List<TestdataGroupSplitValue> alignedValues) {
        this.alignedValues = alignedValues;
    }

    public TestdataGroupSplitValue getPeer() {
        return peer;
    }

    public void setPeer(TestdataGroupSplitValue peer) {
        this.peer = peer;
    }

    public List<TestdataGroupSplitValue> getFollowers() {
        return followers;
    }

    public void setFollowers(List<TestdataGroupSplitValue> followers) {
        this.followers = followers;
    }

    public TestdataGroupSplitValue getPreviousValue() {
        return previousValue;
    }

    public void setPreviousValue(TestdataGroupSplitValue previousValue) {
        this.previousValue = previousValue;
    }

    public TestdataGroupSplitEntity getEntity() {
        return entity;
    }

    public void setEntity(TestdataGroupSplitEntity entity) {
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

    public LocalDateTime getHandoverTime() {
        return handoverTime;
    }

    public void setHandoverTime(LocalDateTime handoverTime) {
        this.handoverTime = handoverTime;
    }

    @Override
    public String toString() {
        return id + "{" +
                "startTime=" + startTime +
                ", endTime=" + endTime +
                '}';
    }
}
