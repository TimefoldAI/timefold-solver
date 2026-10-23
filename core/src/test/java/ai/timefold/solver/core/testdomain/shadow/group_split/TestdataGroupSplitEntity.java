package ai.timefold.solver.core.testdomain.shadow.group_split;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.PlanningListVariable;

@PlanningEntity
public class TestdataGroupSplitEntity {
    String id;

    @PlanningListVariable
    List<TestdataGroupSplitValue> values;

    LocalDateTime startTime;

    public TestdataGroupSplitEntity() {
    }

    public TestdataGroupSplitEntity(String id, LocalDateTime startTime) {
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

    public List<TestdataGroupSplitValue> getValues() {
        return values;
    }

    public void setValues(List<TestdataGroupSplitValue> values) {
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
