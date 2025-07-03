package ai.timefold.solver.core.testdomain.declarative.multi_entity;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.InverseRelationShadowVariable;
import ai.timefold.solver.core.api.domain.variable.PreviousElementShadowVariable;
import ai.timefold.solver.core.api.domain.variable.ShadowVariable;
import ai.timefold.solver.core.preview.api.domain.variable.declarative.ShadowSources;
import ai.timefold.solver.core.preview.api.domain.variable.declarative.ShadowVariableLooped;

import org.apache.commons.lang3.ObjectUtils;

@PlanningEntity
public class TestdataMultiEntityDependencyValue {
    String id;
    List<TestdataMultiEntityDependencyValue> dependencies;

    @PreviousElementShadowVariable(sourceVariableName = "values")
    TestdataMultiEntityDependencyValue previousValue;

    @ShadowVariable(supplierName = "calculateStartTime")
    LocalDateTime startTime;

    @ShadowVariable(supplierName = "calculateEndTime")
    LocalDateTime endTime;

    @ShadowVariableLooped
    boolean isInvalid;

    @InverseRelationShadowVariable(sourceVariableName = "values")
    TestdataMultiEntityDependencyEntity entity;

    Duration duration;

    public TestdataMultiEntityDependencyValue() {
    }

    public TestdataMultiEntityDependencyValue(String id, Duration duration) {
        this(id, duration, null);
    }

    public TestdataMultiEntityDependencyValue(String id, Duration duration,
            List<TestdataMultiEntityDependencyValue> dependencies) {
        this.id = id;
        this.duration = duration;
        this.dependencies = dependencies;
    }

    public List<TestdataMultiEntityDependencyValue> getDependencies() {
        return dependencies;
    }

    public void setDependencies(
            List<TestdataMultiEntityDependencyValue> dependencies) {
        this.dependencies = dependencies;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    @ShadowSources({ "dependencies[].endTime", "previousValue.endTime", "entity.readyTime" })
    public LocalDateTime calculateStartTime() {
        LocalDateTime readyTime;
        if (previousValue != null) {
            readyTime = previousValue.endTime;
        } else if (entity != null) {
            readyTime = entity.readyTime;
            if (readyTime == null) {
                return null;
            }
        } else {
            return null;
        }

        if (dependencies != null) {
            for (var dependency : dependencies) {
                if (dependency.endTime == null) {
                    return null;
                }
                readyTime = ObjectUtils.max(readyTime, dependency.endTime);
            }
        }
        return readyTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    @ShadowSources({ "startTime" })
    public LocalDateTime calculateEndTime() {
        if (startTime == null) {
            return null;
        }
        return startTime.plus(duration);
    }

    public Duration getDuration() {
        return duration;
    }

    public void setDuration(Duration duration) {
        this.duration = duration;
    }

    public boolean isInvalid() {
        return isInvalid;
    }

    public void setInvalid(boolean invalid) {
        isInvalid = invalid;
    }

    public TestdataMultiEntityDependencyEntity getEntity() {
        return entity;
    }

    public void setEntity(TestdataMultiEntityDependencyEntity entity) {
        this.entity = entity;
    }

    @Override
    public String toString() {
        return id + "{" +
                "endTime=" + endTime +
                '}';
    }
}
