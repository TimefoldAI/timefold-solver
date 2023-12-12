package ai.timefold.solver.core.impl.testdata.domain.list.pinned;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.entity.PlanningPin;
import ai.timefold.solver.core.api.domain.variable.PlanningListVariable;
import ai.timefold.solver.core.impl.domain.entity.descriptor.EntityDescriptor;
import ai.timefold.solver.core.impl.domain.variable.descriptor.ListVariableDescriptor;
import ai.timefold.solver.core.impl.testdata.domain.TestdataObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@PlanningEntity
public class TestdataPinnedListEntity extends TestdataObject {

    public static EntityDescriptor<TestdataPinnedListSolution> buildEntityDescriptor() {
        return TestdataPinnedListSolution.buildSolutionDescriptor().findEntityDescriptorOrFail(TestdataPinnedListEntity.class);
    }

    public static ListVariableDescriptor<TestdataPinnedListSolution> buildVariableDescriptorForValueList() {
        return (ListVariableDescriptor<TestdataPinnedListSolution>) buildEntityDescriptor().getGenuineVariableDescriptor("valueList");
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

    @PlanningListVariable(valueRangeProviderRefs = "valueRange")
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

    public List<TestdataPinnedListValue> getValueList() {
        return valueList;
    }

    public boolean isPinned() {
        return pinned;
    }

    public void setPinned(boolean pinned) {
        this.pinned = pinned;
    }
}
