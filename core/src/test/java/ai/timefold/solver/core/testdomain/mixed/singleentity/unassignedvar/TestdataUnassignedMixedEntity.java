package ai.timefold.solver.core.testdomain.mixed.singleentity.unassignedvar;

import java.util.ArrayList;
import java.util.List;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.entity.PlanningPin;
import ai.timefold.solver.core.api.domain.entity.PlanningPinToIndex;
import ai.timefold.solver.core.api.domain.variable.PlanningListVariable;
import ai.timefold.solver.core.api.domain.variable.PlanningVariable;
import ai.timefold.solver.core.testdomain.TestdataObject;

@PlanningEntity
public class TestdataUnassignedMixedEntity extends TestdataObject {

    @PlanningVariable(valueRangeProviderRefs = "otherValueRange", allowsUnassigned = true)
    private TestdataUnassignedMixedOtherValue basicValue;

    @PlanningVariable(valueRangeProviderRefs = "otherValueRange")
    private TestdataUnassignedMixedOtherValue secondBasicValue;

    @PlanningListVariable(valueRangeProviderRefs = "valueRange", allowsUnassignedValues = true)
    private List<TestdataUnassignedMixedValue> valueList;

    @PlanningPin
    private boolean pinned = false;

    @PlanningPinToIndex
    private int pinnedIndex = 0;

    public TestdataUnassignedMixedEntity() {
        // Required for cloner
    }

    public TestdataUnassignedMixedEntity(String code) {
        super(code);
        valueList = new ArrayList<>();
    }

    public TestdataUnassignedMixedOtherValue getBasicValue() {
        return basicValue;
    }

    public void setBasicValue(TestdataUnassignedMixedOtherValue basicValue) {
        this.basicValue = basicValue;
    }

    public TestdataUnassignedMixedOtherValue getSecondBasicValue() {
        return secondBasicValue;
    }

    public void setSecondBasicValue(TestdataUnassignedMixedOtherValue secondBasicValue) {
        this.secondBasicValue = secondBasicValue;
    }

    public List<TestdataUnassignedMixedValue> getValueList() {
        return valueList;
    }

    public void setValueList(List<TestdataUnassignedMixedValue> valueList) {
        this.valueList = valueList;
    }

    public boolean isPinned() {
        return pinned;
    }

    public void setPinned(boolean pinned) {
        this.pinned = pinned;
    }

    public int getPinnedIndex() {
        return pinnedIndex;
    }

    public void setPinnedIndex(int pinnedIndex) {
        this.pinnedIndex = pinnedIndex;
    }
}
