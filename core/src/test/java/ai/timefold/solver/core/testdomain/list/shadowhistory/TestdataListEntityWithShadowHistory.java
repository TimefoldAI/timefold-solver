package ai.timefold.solver.core.testdomain.list.shadowhistory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.PlanningListVariable;
import ai.timefold.solver.core.impl.domain.entity.descriptor.EntityDescriptor;
import ai.timefold.solver.core.impl.domain.variable.descriptor.ListVariableDescriptor;
import ai.timefold.solver.core.testdomain.TestdataObject;

@PlanningEntity
public class TestdataListEntityWithShadowHistory extends TestdataObject {

    public static EntityDescriptor<TestdataListSolutionWithShadowHistory> buildEntityDescriptor() {
        return TestdataListSolutionWithShadowHistory.buildSolutionDescriptor()
                .findEntityDescriptorOrFail(TestdataListEntityWithShadowHistory.class);
    }

    public static ListVariableDescriptor<TestdataListSolutionWithShadowHistory> buildVariableDescriptorForValueList() {
        return (ListVariableDescriptor<TestdataListSolutionWithShadowHistory>) buildEntityDescriptor()
                .getGenuineVariableDescriptor("valueList");
    }

    @PlanningListVariable(valueRangeProviderRefs = "valueRange")
    private List<TestdataListValueWithShadowHistory> valueList;

    public TestdataListEntityWithShadowHistory() {
    }

    public TestdataListEntityWithShadowHistory(String code, List<TestdataListValueWithShadowHistory> valueList) {
        super(code);
        this.valueList = valueList;
    }

    public TestdataListEntityWithShadowHistory(String code, TestdataListValueWithShadowHistory... values) {
        this(code, new ArrayList<>(Arrays.asList(values)));
    }

    public List<TestdataListValueWithShadowHistory> getValueList() {
        return valueList;
    }

    public void setValueList(
            List<TestdataListValueWithShadowHistory> valueList) {
        this.valueList = valueList;
    }
}
