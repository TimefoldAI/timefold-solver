package ai.timefold.solver.core.testdomain.shadow.no_inconsistent_field;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import ai.timefold.solver.core.api.domain.common.PlanningId;
import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.InverseRelationShadowVariable;
import ai.timefold.solver.core.api.domain.variable.PreviousElementShadowVariable;
import ai.timefold.solver.core.api.domain.variable.ShadowSources;
import ai.timefold.solver.core.api.domain.variable.ShadowVariable;

@PlanningEntity
public class TestdataDependencyNoInconsistentFieldValue {
    @PlanningId
    String id;
    List<TestdataDependencyNoInconsistentFieldValue> dependencies;

    @PreviousElementShadowVariable(sourceVariableName = "values")
    TestdataDependencyNoInconsistentFieldValue previousValue;

    @ShadowVariable(supplierName = "calculateStartTime")
    LocalDateTime startTime;

    @ShadowVariable(supplierName = "calculateEndTime")
    LocalDateTime endTime;

    @InverseRelationShadowVariable(sourceVariableName = "values")
    TestdataDependencyNoInconsistentFieldEntity entity;

    Duration duration;

    public TestdataDependencyNoInconsistentFieldValue() {
    }

    public TestdataDependencyNoInconsistentFieldValue(String id) {
        this(id, Duration.ofHours(1L), null);
    }

    public TestdataDependencyNoInconsistentFieldValue(String id, Duration duration) {
        this(id, duration, null);
    }

    public TestdataDependencyNoInconsistentFieldValue(String id, Duration duration,
            List<TestdataDependencyNoInconsistentFieldValue> dependencies) {
        this.id = id;
        this.duration = duration;
        this.dependencies = dependencies;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public List<TestdataDependencyNoInconsistentFieldValue> getDependencies() {
        return dependencies;
    }

    public void setDependencies(
            List<TestdataDependencyNoInconsistentFieldValue> dependencies) {
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
                readyTime = readyTime.isAfter(dependency.endTime) ? readyTime : dependency.endTime;
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

    public TestdataDependencyNoInconsistentFieldEntity getEntity() {
        return entity;
    }

    public void setEntity(TestdataDependencyNoInconsistentFieldEntity entity) {
        this.entity = entity;
    }

    public TestdataDependencyNoInconsistentFieldValue getPreviousValue() {
        return previousValue;
    }

    public void setPreviousValue(TestdataDependencyNoInconsistentFieldValue previousValue) {
        this.previousValue = previousValue;
    }

    @Override
    public String toString() {
        return "%s{endTime=%s}".formatted(id, endTime);
    }
}
