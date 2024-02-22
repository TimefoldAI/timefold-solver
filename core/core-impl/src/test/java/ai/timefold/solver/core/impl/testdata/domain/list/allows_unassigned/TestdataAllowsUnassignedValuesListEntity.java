package ai.timefold.solver.core.impl.testdata.domain.list.allows_unassigned;

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
        for (int i = 0; i < valueList.size(); i++) {
            var testdataListValue = valueList.get(i);
            testdataListValue.setEntity(this);
            testdataListValue.setIndex(valueList.indexOf(testdataListValue));
            if (i != 0) {
                testdataListValue.setPrevious(valueList.get(i - 1));
            }
            if (i != valueList.size() - 1) {
                testdataListValue.setNext(valueList.get(i + 1));
            }
        }
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
