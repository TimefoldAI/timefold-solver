package ai.timefold.solver.quarkus.testdomain.cascade;

import java.util.ArrayList;
import java.util.List;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.PlanningListVariable;

@PlanningEntity
public class TestdataQuarkusDuplicateCascadingEntity {
    String id;

    @PlanningListVariable
    List<TestdataQuarkusDuplicateCascadingValue> valueList;

    public TestdataQuarkusDuplicateCascadingEntity() {
        valueList = new ArrayList<>();
    }

    public TestdataQuarkusDuplicateCascadingEntity(String id) {
        this.id = id;
        valueList = new ArrayList<>();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public List<TestdataQuarkusDuplicateCascadingValue> getValueList() {
        return valueList;
    }

    public void setValueList(List<TestdataQuarkusDuplicateCascadingValue> valueList) {
        this.valueList = valueList;
    }
}
