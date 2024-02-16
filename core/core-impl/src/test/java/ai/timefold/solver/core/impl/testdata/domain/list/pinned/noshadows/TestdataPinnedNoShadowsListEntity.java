package ai.timefold.solver.core.impl.testdata.domain.list.pinned.noshadows;

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
public class TestdataPinnedNoShadowsListEntity extends TestdataObject {

    public static EntityDescriptor<TestdataPinnedNoShadowsListSolution> buildEntityDescriptor() {
        return TestdataPinnedNoShadowsListSolution.buildSolutionDescriptor()
                .findEntityDescriptorOrFail(TestdataPinnedNoShadowsListEntity.class);
    }

    public static ListVariableDescriptor<TestdataPinnedNoShadowsListSolution> buildVariableDescriptorForValueList() {
        return (ListVariableDescriptor<TestdataPinnedNoShadowsListSolution>) buildEntityDescriptor()
                .getGenuineVariableDescriptor("valueList");
    }

    public static TestdataPinnedNoShadowsListEntity createWithValues(String code, TestdataPinnedNoShadowsListValue... values) {
        // Set up shadow variables to preserve consistency.
        return new TestdataPinnedNoShadowsListEntity(code, values).setUpShadowVariables();
    }

    TestdataPinnedNoShadowsListEntity setUpShadowVariables() {
        valueList.forEach(testdataListValue -> {
            testdataListValue.setIndex(valueList.indexOf(testdataListValue));
        });
        return this;
    }

    private List<TestdataPinnedNoShadowsListValue> valueList;

    @PlanningPin
    private boolean pinned;

    public TestdataPinnedNoShadowsListEntity() {
    }

    public TestdataPinnedNoShadowsListEntity(String code, List<TestdataPinnedNoShadowsListValue> valueList) {
        super(code);
        this.valueList = valueList;
    }

    public TestdataPinnedNoShadowsListEntity(String code, TestdataPinnedNoShadowsListValue... values) {
        this(code, new ArrayList<>(Arrays.asList(values)));
    }

    @PlanningListVariable(valueRangeProviderRefs = "valueRange")
    public List<TestdataPinnedNoShadowsListValue> getValueList() {
        if (pinned) {
            return Collections.unmodifiableList(valueList); // Hard fail when something tries to modify the list.
        }
        return valueList;
    }

    public void setValueList(List<TestdataPinnedNoShadowsListValue> valueList) {
        this.valueList = valueList;
    }

    public boolean isPinned() {
        return pinned;
    }

    public void setPinned(boolean pinned) {
        this.pinned = pinned;
    }
}
