package ai.timefold.solver.core.impl.testdata.domain.list.pinned.index;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.entity.PlanningPin;
import ai.timefold.solver.core.api.domain.entity.PlanningPinToIndex;
import ai.timefold.solver.core.api.domain.variable.PlanningListVariable;
import ai.timefold.solver.core.impl.domain.entity.descriptor.EntityDescriptor;
import ai.timefold.solver.core.impl.domain.variable.descriptor.ListVariableDescriptor;
import ai.timefold.solver.core.impl.testdata.domain.TestdataObject;

@PlanningEntity
public class TestdataPinnedWithIndexListEntity extends TestdataObject {

    public static EntityDescriptor<TestdataPinnedWithIndexListSolution> buildEntityDescriptor() {
        return TestdataPinnedWithIndexListSolution.buildSolutionDescriptor()
                .findEntityDescriptorOrFail(TestdataPinnedWithIndexListEntity.class);
    }

    public static ListVariableDescriptor<TestdataPinnedWithIndexListSolution> buildVariableDescriptorForValueList() {
        return (ListVariableDescriptor<TestdataPinnedWithIndexListSolution>) buildEntityDescriptor()
                .getGenuineVariableDescriptor("valueList");
    }

    public static TestdataPinnedWithIndexListEntity createWithValues(String code, TestdataPinnedWithIndexListValue... values) {
        // Set up shadow variables to preserve consistency.
        return new TestdataPinnedWithIndexListEntity(code, values).setUpShadowVariables();
    }

    TestdataPinnedWithIndexListEntity setUpShadowVariables() {
        valueList.forEach(testdataListValue -> {
            testdataListValue.setEntity(this);
        });
        return this;
    }

    private List<TestdataPinnedWithIndexListValue> valueList;

    @PlanningPin
    private boolean pinned;

    @PlanningPinToIndex
    private int pinIndex;

    public TestdataPinnedWithIndexListEntity() {
    }

    public TestdataPinnedWithIndexListEntity(String code, List<TestdataPinnedWithIndexListValue> valueList) {
        super(code);
        this.valueList = valueList;
    }

    public TestdataPinnedWithIndexListEntity(String code, TestdataPinnedWithIndexListValue... values) {
        this(code, new ArrayList<>(Arrays.asList(values)));
    }

    @PlanningListVariable(valueRangeProviderRefs = "valueRange")
    public List<TestdataPinnedWithIndexListValue> getValueList() {
        if (pinned) {
            return Collections.unmodifiableList(valueList); // Hard fail when something tries to modify the list.
        }
        return valueList;
    }

    public void setValueList(List<TestdataPinnedWithIndexListValue> valueList) {
        this.valueList = valueList;
    }

    public boolean isPinned() {
        return pinned;
    }

    public void setPinned(boolean pinned) {
        this.pinned = pinned;
    }

    public int getPinIndex() {
        return pinIndex;
    }

    public void setPlanningPinToIndex(int pinIndex) {
        this.pinIndex = pinIndex;
    }

}
