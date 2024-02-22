package ai.timefold.solver.core.impl.testdata.domain.list.pinned;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.entity.PlanningPin;
import ai.timefold.solver.core.api.domain.variable.PlanningListVariable;
import ai.timefold.solver.core.impl.domain.entity.descriptor.EntityDescriptor;
import ai.timefold.solver.core.impl.domain.variable.descriptor.ListVariableDescriptor;
import ai.timefold.solver.core.impl.testdata.domain.TestdataObject;

@PlanningEntity
public class TestdataPinnedListEntity extends TestdataObject {

    public static EntityDescriptor<TestdataPinnedListSolution> buildEntityDescriptor() {
        return TestdataPinnedListSolution.buildSolutionDescriptor().findEntityDescriptorOrFail(TestdataPinnedListEntity.class);
    }

    public static ListVariableDescriptor<TestdataPinnedListSolution> buildVariableDescriptorForValueList() {
        return (ListVariableDescriptor<TestdataPinnedListSolution>) buildEntityDescriptor()
                .getGenuineVariableDescriptor("valueList");
    }

    public static TestdataPinnedListEntity createWithValues(String code, TestdataPinnedListValue... values) {
        // Set up shadow variables to preserve consistency.
        return new TestdataPinnedListEntity(code, values).setUpShadowVariables();
    }

    TestdataPinnedListEntity setUpShadowVariables() {
        valueList.forEach(testdataListValue -> {
            testdataListValue.setEntity(this);
            testdataListValue.setIndex(valueList.indexOf(testdataListValue));
        });
        return this;
    }

    private List<TestdataPinnedListValue> valueList;

    @PlanningPin
    private boolean pinned;

    public TestdataPinnedListEntity() {
    }

    public TestdataPinnedListEntity(String code, List<TestdataPinnedListValue> valueList) {
        super(code);
        this.valueList = valueList;
    }

    public TestdataPinnedListEntity(String code, TestdataPinnedListValue... values) {
        this(code, new ArrayList<>(Arrays.asList(values)));
    }

    @PlanningListVariable(valueRangeProviderRefs = "valueRange")
    public List<TestdataPinnedListValue> getValueList() {
        if (pinned) {
            return Collections.unmodifiableList(valueList); // Hard fail when something tries to modify the list.
        }
        return valueList;
    }

    public void setValueList(List<TestdataPinnedListValue> valueList) {
        this.valueList = valueList;
    }

    public boolean isPinned() {
        return pinned;
    }

    public void setPinned(boolean pinned) {
        this.pinned = pinned;
    }
}
