package ai.timefold.solver.core.testdomain.declarative.dependency;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.PlanningListVariable;

@PlanningEntity
public class TestdataDependencyEntity {
    @PlanningListVariable
    List<TestdataDependencyValue> values;

    LocalDateTime startTime;

    public TestdataDependencyEntity() {
        this(LocalDateTime.ofEpochSecond(0L, 0, ZoneOffset.UTC));
    }

    public TestdataDependencyEntity(LocalDateTime startTime) {
        this.startTime = startTime;
        this.values = new ArrayList<>();
    }

    public List<TestdataDependencyValue> getValues() {
        return values;
    }

    public void setValues(List<TestdataDependencyValue> values) {
        this.values = values;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    @Override
    public String toString() {
        return "TestdataPredecessorEntity{" +
                "values=" + values +
                ", startTime=" + startTime +
                '}';
    }
}
