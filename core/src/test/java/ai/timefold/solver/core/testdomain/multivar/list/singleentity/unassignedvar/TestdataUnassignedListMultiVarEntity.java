package ai.timefold.solver.core.testdomain.multivar.list.singleentity.unassignedvar;

import java.util.ArrayList;
import java.util.List;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.PlanningListVariable;
import ai.timefold.solver.core.api.domain.variable.PlanningVariable;
import ai.timefold.solver.core.testdomain.TestdataObject;

@PlanningEntity
public class TestdataUnassignedListMultiVarEntity extends TestdataObject {

    @PlanningVariable(valueRangeProviderRefs = "otherValueRange", allowsUnassigned = true)
    private TestdataUnassignedListMultiVarOtherValue basicValue;

    @PlanningVariable(valueRangeProviderRefs = "otherValueRange")
    private TestdataUnassignedListMultiVarOtherValue secondBasicValue;

    @PlanningListVariable(valueRangeProviderRefs = "valueRange", allowsUnassignedValues = true)
    private List<TestdataUnassignedListMultiVarValue> valueList;

    public TestdataUnassignedListMultiVarEntity() {
        // Required for cloner
    }

    public TestdataUnassignedListMultiVarEntity(String code) {
        super(code);
        valueList = new ArrayList<>();
    }

    public TestdataUnassignedListMultiVarOtherValue getBasicValue() {
        return basicValue;
    }

    public void setBasicValue(TestdataUnassignedListMultiVarOtherValue basicValue) {
        this.basicValue = basicValue;
    }

    public TestdataUnassignedListMultiVarOtherValue getSecondBasicValue() {
        return secondBasicValue;
    }

    public void setSecondBasicValue(TestdataUnassignedListMultiVarOtherValue secondBasicValue) {
        this.secondBasicValue = secondBasicValue;
    }

    public List<TestdataUnassignedListMultiVarValue> getValueList() {
        return valueList;
    }

    public void setValueList(List<TestdataUnassignedListMultiVarValue> valueList) {
        this.valueList = valueList;
    }
}
