package ai.timefold.solver.core.testdomain.shadow.aligned_dependency;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.PlanningListVariable;

@PlanningEntity
public class TestdataAlignedDependencyEntity {
    String id;

    @PlanningListVariable
    List<TestdataAlignedDependencyValue> values;

    LocalDateTime startTime;

    public TestdataAlignedDependencyEntity() {
    }

    public TestdataAlignedDependencyEntity(String id, LocalDateTime startTime) {
        this.id = id;
        this.startTime = startTime;
        this.values = new ArrayList<>();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public List<TestdataAlignedDependencyValue> getValues() {
        return values;
    }

    public void setValues(List<TestdataAlignedDependencyValue> values) {
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
        return id + "{" +
                "values=" + values +
                '}';
    }
}
