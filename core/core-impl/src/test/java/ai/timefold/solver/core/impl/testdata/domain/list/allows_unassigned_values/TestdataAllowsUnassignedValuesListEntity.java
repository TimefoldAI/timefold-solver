package ai.timefold.solver.core.impl.testdata.domain.list.allows_unassigned_values;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.PlanningListVariable;
import ai.timefold.solver.core.impl.domain.entity.descriptor.EntityDescriptor;
import ai.timefold.solver.core.impl.domain.variable.descriptor.ListVariableDescriptor;
import ai.timefold.solver.core.impl.testdata.domain.TestdataObject;

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

    public static TestdataAllowsUnassignedValuesListEntity createWithValues(String code,
            TestdataAllowsUnassignedValuesListValue... values) {
        // Set up shadow variables to preserve consistency.
        return new TestdataAllowsUnassignedValuesListEntity(code, values).setUpShadowVariables();
    }

    TestdataAllowsUnassignedValuesListEntity setUpShadowVariables() {
        valueList.forEach(testdataListValue -> {
            testdataListValue.setEntity(this);
            testdataListValue.setIndex(valueList.indexOf(testdataListValue));
        });
        return this;
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
