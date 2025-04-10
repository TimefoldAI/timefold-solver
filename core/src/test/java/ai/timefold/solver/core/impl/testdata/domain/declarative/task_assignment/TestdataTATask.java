package ai.timefold.solver.core.impl.testdata.domain.declarative.task_assignment;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.InverseRelationShadowVariable;
import ai.timefold.solver.core.api.domain.variable.PreviousElementShadowVariable;
import ai.timefold.solver.core.api.domain.variable.ShadowVariable;
import ai.timefold.solver.core.preview.api.domain.variable.declarative.InvalidityMarker;
import ai.timefold.solver.core.preview.api.domain.variable.declarative.ShadowVariableUpdater;

import org.apache.commons.lang3.ObjectUtils;

@PlanningEntity
public class TestdataTATask {
    String id;
    List<TestdataTATask> dependencies;

    @PreviousElementShadowVariable(sourceVariableName = "tasks")
    TestdataTATask previousTask;

    @ShadowVariable(method = "calculateStartTime")
    LocalDateTime startTime;

    @ShadowVariable(method = "calculateEndTime")
    LocalDateTime endTime;

    @InvalidityMarker
    boolean isInvalid;

    @InverseRelationShadowVariable(sourceVariableName = "tasks")
    TestdataTAEmployee employee;

    Duration duration;

    public TestdataTATask() {
    }

    public TestdataTATask(String id, Duration duration) {
        this(id, duration, null);
    }

    public TestdataTATask(String id, Duration duration, List<TestdataTATask> dependencies) {
        this.id = id;
        this.duration = duration;
        this.dependencies = dependencies;
    }

    public List<TestdataTATask> getDependencies() {
        return dependencies;
    }

    public void setDependencies(
            List<TestdataTATask> dependencies) {
        this.dependencies = dependencies;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    @ShadowVariableUpdater(sources = { "dependencies[].endTime", "previousTask.endTime", "employee" })
    public LocalDateTime calculateStartTime() {
        LocalDateTime readyTime;
        if (previousTask != null) {
            readyTime = previousTask.endTime;
        } else if (employee != null) {
            readyTime = employee.startTime;
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

    @ShadowVariableUpdater(sources = { "startTime" })
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

    public TestdataTAEmployee getEmployee() {
        return employee;
    }

    public void setEmployee(TestdataTAEmployee employee) {
        this.employee = employee;
    }

    @Override
    public String toString() {
        return id + "{" +
                "endTime=" + endTime +
                '}';
    }
}
