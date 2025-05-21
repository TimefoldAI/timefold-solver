package ai.timefold.solver.core.testdomain.multivar.list.singleentity;

import java.util.ArrayList;
import java.util.List;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.entity.PlanningPin;
import ai.timefold.solver.core.api.domain.entity.PlanningPinToIndex;
import ai.timefold.solver.core.api.domain.variable.PlanningListVariable;
import ai.timefold.solver.core.api.domain.variable.PlanningVariable;
import ai.timefold.solver.core.testdomain.TestdataObject;

@PlanningEntity
public class TestdataListMultiVarEntity extends TestdataObject {

    @PlanningVariable(valueRangeProviderRefs = "otherValueRange")
    private TestdataListMultiVarOtherValue basicValue;

    @PlanningVariable(valueRangeProviderRefs = "otherValueRange")
    private TestdataListMultiVarOtherValue secondBasicValue;

    @PlanningListVariable(valueRangeProviderRefs = "valueRange")
    private List<TestdataListMultiVarValue> valueList;

    @PlanningPin
    private boolean pinned = false;

    @PlanningPinToIndex
    private int pinnedIndex = 0;

    public TestdataListMultiVarEntity() {
        // Required for cloner
    }

    public TestdataListMultiVarEntity(String code) {
        super(code);
        valueList = new ArrayList<>();
    }

    public TestdataListMultiVarOtherValue getBasicValue() {
        return basicValue;
    }

    public void setBasicValue(TestdataListMultiVarOtherValue basicValue) {
        this.basicValue = basicValue;
    }

    public TestdataListMultiVarOtherValue getSecondBasicValue() {
        return secondBasicValue;
    }

    public void setSecondBasicValue(TestdataListMultiVarOtherValue secondBasicValue) {
        this.secondBasicValue = secondBasicValue;
    }

    public List<TestdataListMultiVarValue> getValueList() {
        return valueList;
    }

    public void setValueList(List<TestdataListMultiVarValue> valueList) {
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
