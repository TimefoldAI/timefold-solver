package ai.timefold.solver.core.testdomain.shadow.aligned_dependency;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.PlanningListVariable;

@PlanningEntity
public class TestdataAlignedDependencyEntity {
    String id;

    @PlanningListVariable
    List<TestdataAlignedDependencyValue> values;

    OffsetDateTime startTime;

    public TestdataAlignedDependencyEntity() {
    }

    public TestdataAlignedDependencyEntity(String id, OffsetDateTime startTime) {
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

    public OffsetDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(OffsetDateTime startTime) {
        this.startTime = startTime;
    }

    @Override
    public String toString() {
        return id + "{" +
                "values=" + values +
                '}';
    }
}
