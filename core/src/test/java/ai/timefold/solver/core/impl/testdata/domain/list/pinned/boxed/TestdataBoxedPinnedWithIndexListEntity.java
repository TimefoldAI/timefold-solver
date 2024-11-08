package ai.timefold.solver.core.impl.testdata.domain.list.pinned.boxed;

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
public class TestdataBoxedPinnedWithIndexListEntity extends TestdataObject {

    public static EntityDescriptor<TestdataBoxedPinnedWithIndexListSolution> buildEntityDescriptor() {
        return TestdataBoxedPinnedWithIndexListSolution.buildSolutionDescriptor()
                .findEntityDescriptorOrFail(TestdataBoxedPinnedWithIndexListEntity.class);
    }

    public static ListVariableDescriptor<TestdataBoxedPinnedWithIndexListSolution> buildVariableDescriptorForValueList() {
        return (ListVariableDescriptor<TestdataBoxedPinnedWithIndexListSolution>) buildEntityDescriptor()
                .getGenuineVariableDescriptor("valueList");
    }

    public static TestdataBoxedPinnedWithIndexListEntity createWithValues(String code,
            TestdataBoxedPinnedWithIndexListValue... values) {
        // Set up shadow variables to preserve consistency.
        return new TestdataBoxedPinnedWithIndexListEntity(code, values).setUpShadowVariables();
    }

    TestdataBoxedPinnedWithIndexListEntity setUpShadowVariables() {
        valueList.forEach(testdataListValue -> {
            testdataListValue.setEntity(this);
        });
        return this;
    }

    private List<TestdataBoxedPinnedWithIndexListValue> valueList;

    @PlanningPin
    private boolean pinned;

    @PlanningPinToIndex
    private Integer pinIndex;

    public TestdataBoxedPinnedWithIndexListEntity() {
    }

    public TestdataBoxedPinnedWithIndexListEntity(String code, List<TestdataBoxedPinnedWithIndexListValue> valueList) {
        super(code);
        this.valueList = valueList;
    }

    public TestdataBoxedPinnedWithIndexListEntity(String code, TestdataBoxedPinnedWithIndexListValue... values) {
        this(code, new ArrayList<>(Arrays.asList(values)));
    }

    @PlanningListVariable(valueRangeProviderRefs = "valueRange")
    public List<TestdataBoxedPinnedWithIndexListValue> getValueList() {
        if (pinned) {
            return Collections.unmodifiableList(valueList); // Hard fail when something tries to modify the list.
        }
        return valueList;
    }

    public void setValueList(List<TestdataBoxedPinnedWithIndexListValue> valueList) {
        this.valueList = valueList;
    }

    public boolean isPinned() {
        return pinned;
    }

    public void setPinned(boolean pinned) {
        this.pinned = pinned;
    }

    public Integer getPinIndex() {
        return pinIndex;
    }

    public void setPlanningPinToIndex(Integer pinIndex) {
        this.pinIndex = pinIndex;
    }

}
