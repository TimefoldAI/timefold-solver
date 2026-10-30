package ai.timefold.solver.core.testdomain.shadow.group_chain;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.PlanningListVariable;

@PlanningEntity
public class TestdataGroupChainEntity {
    String id;

    @PlanningListVariable
    List<TestdataGroupChainValue> values;

    OffsetDateTime startTime;

    public TestdataGroupChainEntity() {
    }

    public TestdataGroupChainEntity(String id, OffsetDateTime startTime) {
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

    public List<TestdataGroupChainValue> getValues() {
        return values;
    }

    public void setValues(List<TestdataGroupChainValue> values) {
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
