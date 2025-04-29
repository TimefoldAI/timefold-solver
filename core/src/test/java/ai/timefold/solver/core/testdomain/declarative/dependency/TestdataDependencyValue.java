package ai.timefold.solver.core.testdomain.declarative.dependency;

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
public class TestdataDependencyValue {
    String id;
    List<TestdataDependencyValue> dependencies;

    @PreviousElementShadowVariable(sourceVariableName = "values")
    TestdataDependencyValue previousValue;

    @ShadowVariable(supplierName = "calculateStartTime")
    LocalDateTime startTime;

    @ShadowVariable(supplierName = "calculateEndTime")
    LocalDateTime endTime;

    @ShadowVariableLooped
    boolean isInvalid;

    @InverseRelationShadowVariable(sourceVariableName = "values")
    TestdataDependencyEntity entity;

    Duration duration;

    public TestdataDependencyValue() {
    }

    public TestdataDependencyValue(String id, Duration duration) {
        this(id, duration, null);
    }

    public TestdataDependencyValue(String id, Duration duration, List<TestdataDependencyValue> dependencies) {
        this.id = id;
        this.duration = duration;
        this.dependencies = dependencies;
    }

    public List<TestdataDependencyValue> getDependencies() {
        return dependencies;
    }

    public void setDependencies(
            List<TestdataDependencyValue> dependencies) {
        this.dependencies = dependencies;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    @ShadowSources({ "dependencies[].endTime", "previousValue.endTime", "entity" })
    public LocalDateTime calculateStartTime() {
        LocalDateTime readyTime;
        if (previousValue != null) {
            readyTime = previousValue.endTime;
        } else if (entity != null) {
            readyTime = entity.startTime;
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

    public TestdataDependencyEntity getEntity() {
        return entity;
    }

    public void setEntity(TestdataDependencyEntity entity) {
        this.entity = entity;
    }

    @Override
    public String toString() {
        return id + "{" +
                "endTime=" + endTime +
                '}';
    }
}
