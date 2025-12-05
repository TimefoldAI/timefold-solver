package ai.timefold.solver.spring.boot.it.domain;

import java.util.List;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.lookup.PlanningId;
import ai.timefold.solver.core.api.domain.valuerange.ValueRangeProvider;
import ai.timefold.solver.core.api.domain.variable.PlanningVariable;

@PlanningEntity
public class IntegrationTestEntity {
    @PlanningId
    private String id;

    @PlanningVariable(valueRangeProviderRefs = { "valueRange", "valueRangeWithParameter" })
    private IntegrationTestValue value;

    private List<IntegrationTestValue> valueList;

    public IntegrationTestEntity() {
    }

    public IntegrationTestEntity(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public IntegrationTestValue getValue() {
        return value;
    }

    public void setValue(IntegrationTestValue value) {
        this.value = value;
    }

    @ValueRangeProvider(id = "valueRange")
    public List<IntegrationTestValue> getValueList() {
        return valueList;
    }

    public void setValueList(List<IntegrationTestValue> valueList) {
        this.valueList = valueList;
    }

    @ValueRangeProvider(id = "valueRangeWithParameter")
    public List<IntegrationTestValue> getValueRangeWithParameter(IntegrationTestSolution solution) {
        return solution.getValueList();
    }
}
