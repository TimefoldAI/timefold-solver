package ai.timefold.solver.core.impl.testdata.domain.declarative.task_assignment;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.PlanningListVariable;

@PlanningEntity
public class TestdataTAEmployee {
    @PlanningListVariable
    List<TestdataTATask> tasks;

    LocalDateTime startTime;

    public TestdataTAEmployee() {
        this(LocalDateTime.ofEpochSecond(0L, 0, ZoneOffset.UTC));
    }

    public TestdataTAEmployee(LocalDateTime startTime) {
        this.startTime = startTime;
        this.tasks = new ArrayList<>();
    }

    public List<TestdataTATask> getTasks() {
        return tasks;
    }

    public void setTasks(List<TestdataTATask> tasks) {
        this.tasks = tasks;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    @Override
    public String toString() {
        return "TestdataTAEmployee{" +
                "tasks=" + tasks +
                ", startTime=" + startTime +
                '}';
    }
}
