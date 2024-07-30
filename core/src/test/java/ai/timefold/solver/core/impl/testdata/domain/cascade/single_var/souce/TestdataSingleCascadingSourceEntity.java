package ai.timefold.solver.core.impl.testdata.domain.cascade.single_var.souce;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.PlanningListVariable;
import ai.timefold.solver.core.impl.domain.entity.descriptor.EntityDescriptor;
import ai.timefold.solver.core.impl.domain.variable.descriptor.ListVariableDescriptor;
import ai.timefold.solver.core.impl.testdata.domain.TestdataObject;

@PlanningEntity
public class TestdataSingleCascadingSourceEntity extends TestdataObject {

    public static EntityDescriptor<TestdataSingleCascadingSouceSolution> buildEntityDescriptor() {
        return TestdataSingleCascadingSouceSolution.buildSolutionDescriptor()
                .findEntityDescriptorOrFail(TestdataSingleCascadingSourceEntity.class);
    }

    public static ListVariableDescriptor<TestdataSingleCascadingSouceSolution> buildVariableDescriptorForValueList() {
        return (ListVariableDescriptor<TestdataSingleCascadingSouceSolution>) buildEntityDescriptor()
                .getGenuineVariableDescriptor("valueList");
    }

    public static TestdataSingleCascadingSourceEntity createWithValues(String code,
            TestdataSingleCascadingSourceValue... values) {
        // Set up shadow variables to preserve consistency.
        return new TestdataSingleCascadingSourceEntity(code, new ArrayList<>(Arrays.asList(values))).setUpShadowVariables();
    }

    TestdataSingleCascadingSourceEntity setUpShadowVariables() {
        if (valueList != null && !valueList.isEmpty()) {
            for (var v : valueList) {
                v.updateCascadeValue();
            }
        }
        return this;
    }

    @PlanningListVariable(valueRangeProviderRefs = "valueRange")
    private List<TestdataSingleCascadingSourceValue> valueList;

    public TestdataSingleCascadingSourceEntity(String code) {
        super(code);
        this.valueList = new LinkedList<>();
    }

    public TestdataSingleCascadingSourceEntity(String code, List<TestdataSingleCascadingSourceValue> valueList) {
        super(code);
        this.valueList = valueList;
    }

    public void setValueList(List<TestdataSingleCascadingSourceValue> valueList) {
        this.valueList = valueList;
    }

    public List<TestdataSingleCascadingSourceValue> getValueList() {
        return valueList;
    }

    @Override
    public String toString() {
        return "TestdataSingleCascadingSourceEntity{" +
                "code='" + code + '\'' +
                '}';
    }
}
