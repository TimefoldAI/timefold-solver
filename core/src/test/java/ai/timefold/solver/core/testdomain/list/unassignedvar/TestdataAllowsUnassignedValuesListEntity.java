package ai.timefold.solver.core.testdomain.list.unassignedvar;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.PlanningListVariable;
import ai.timefold.solver.core.impl.domain.entity.descriptor.EntityDescriptor;
import ai.timefold.solver.core.impl.domain.variable.descriptor.ListVariableDescriptor;
import ai.timefold.solver.core.testdomain.TestdataObject;

@PlanningEntity
public class TestdataAllowsUnassignedValuesListEntity extends TestdataObject {

    public static EntityDescriptor<TestdataAllowsUnassignedValuesListSolution> buildEntityDescriptor() {
        return TestdataAllowsUnassignedValuesListSolution.buildSolutionDescriptor()
                .findEntityDescriptorOrFail(TestdataAllowsUnassignedValuesListEntity.class);
    }

    public static ListVariableDescriptor<TestdataAllowsUnassignedValuesListSolution> buildVariableDescriptorForValueList() {
        return (ListVariableDescriptor<TestdataAllowsUnassignedValuesListSolution>) buildEntityDescriptor()
                .getGenuineVariableDescriptor("valueList");
    }

    private List<TestdataAllowsUnassignedValuesListValue> valueList;

    public TestdataAllowsUnassignedValuesListEntity() {
    }

    public TestdataAllowsUnassignedValuesListEntity(String code, List<TestdataAllowsUnassignedValuesListValue> valueList) {
        super(code);
        this.valueList = valueList;
    }

    public TestdataAllowsUnassignedValuesListEntity(String code, TestdataAllowsUnassignedValuesListValue... values) {
        this(code, new ArrayList<>(Arrays.asList(values)));
    }

    @PlanningListVariable(allowsUnassignedValues = true, valueRangeProviderRefs = "valueRange")
    public List<TestdataAllowsUnassignedValuesListValue> getValueList() {
        return valueList;
    }

    public void setValueList(List<TestdataAllowsUnassignedValuesListValue> valueList) {
        this.valueList = valueList;
    }

}
