package ai.timefold.solver.core.testdomain.list.valuerange.subset;

import java.util.ArrayList;
import java.util.List;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.PlanningListVariable;
import ai.timefold.solver.core.testdomain.TestdataObject;
import ai.timefold.solver.core.testdomain.TestdataValue;

@PlanningEntity
public class TestdataListSubsetRangeEntity extends TestdataObject {

    @PlanningListVariable(valueRangeProviderRefs = "valueRange", allowsUnassignedValues = true)
    private List<TestdataValue> valueList = new ArrayList<>();

    public TestdataListSubsetRangeEntity() {
    }

    public TestdataListSubsetRangeEntity(String code, TestdataValue... values) {
        super(code);
        valueList = new ArrayList<>(List.of(values));
    }

    public List<TestdataValue> getValueList() {
        return valueList;
    }

    public void setValueList(List<TestdataValue> valueList) {
        this.valueList = valueList;
    }

}
