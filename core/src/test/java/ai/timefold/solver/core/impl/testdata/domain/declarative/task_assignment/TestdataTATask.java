package ai.timefold.solver.core.impl.testdata.domain.declarative.task_assignment;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.InverseRelationShadowVariable;
import ai.timefold.solver.core.preview.api.domain.variable.declarative.DeclarativeShadowVariable;
import ai.timefold.solver.core.preview.api.domain.variable.declarative.InvalidityMarker;

@PlanningEntity
public class TestdataTATask {
    String id;
    List<TestdataTATask> dependencies;

    @DeclarativeShadowVariable(TestdataTAShadowVariableProvider.class)
    LocalDateTime startTime;

    @DeclarativeShadowVariable(TestdataTAShadowVariableProvider.class)
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

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
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
