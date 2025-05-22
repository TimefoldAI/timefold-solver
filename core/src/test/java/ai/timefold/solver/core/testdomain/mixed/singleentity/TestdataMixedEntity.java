package ai.timefold.solver.core.testdomain.mixed.singleentity;

import java.util.ArrayList;
import java.util.List;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.entity.PlanningPin;
import ai.timefold.solver.core.api.domain.entity.PlanningPinToIndex;
import ai.timefold.solver.core.api.domain.variable.PlanningListVariable;
import ai.timefold.solver.core.api.domain.variable.PlanningVariable;
import ai.timefold.solver.core.testdomain.TestdataObject;

@PlanningEntity
public class TestdataMixedEntity extends TestdataObject {

    @PlanningVariable(valueRangeProviderRefs = "otherValueRange")
    private TestdataMixedOtherValue basicValue;

    @PlanningVariable(valueRangeProviderRefs = "otherValueRange")
    private TestdataMixedOtherValue secondBasicValue;

    @PlanningListVariable(valueRangeProviderRefs = "valueRange")
    private List<TestdataMixedValue> valueList;

    @PlanningPin
    private boolean pinned = false;

    @PlanningPinToIndex
    private int pinnedIndex = 0;

    public TestdataMixedEntity() {
        // Required for cloner
    }

    public TestdataMixedEntity(String code) {
        super(code);
        valueList = new ArrayList<>();
    }

    public TestdataMixedOtherValue getBasicValue() {
        return basicValue;
    }

    public void setBasicValue(TestdataMixedOtherValue basicValue) {
        this.basicValue = basicValue;
    }

    public TestdataMixedOtherValue getSecondBasicValue() {
        return secondBasicValue;
    }

    public void setSecondBasicValue(TestdataMixedOtherValue secondBasicValue) {
        this.secondBasicValue = secondBasicValue;
    }

    public List<TestdataMixedValue> getValueList() {
        return valueList;
    }

    public void setValueList(List<TestdataMixedValue> valueList) {
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
