package ai.timefold.solver.core.impl.testdata.domain.cascade.single_var.suply;

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
public class TestdataSingleCascadingWithSupplyEntity extends TestdataObject {

    public static EntityDescriptor<TestdataSingleCascadingWithSupplySolution> buildEntityDescriptor() {
        return TestdataSingleCascadingWithSupplySolution.buildSolutionDescriptor()
                .findEntityDescriptorOrFail(TestdataSingleCascadingWithSupplyEntity.class);
    }

    public static ListVariableDescriptor<TestdataSingleCascadingWithSupplySolution> buildVariableDescriptorForValueList() {
        return (ListVariableDescriptor<TestdataSingleCascadingWithSupplySolution>) buildEntityDescriptor()
                .getGenuineVariableDescriptor("valueList");
    }

    public static TestdataSingleCascadingWithSupplyEntity createWithValues(String code,
            TestdataSingleCascadingWithSupplyValue... values) {
        // Set up shadow variables to preserve consistency.
        return new TestdataSingleCascadingWithSupplyEntity(code, new ArrayList<>(Arrays.asList(values))).setUpShadowVariables();
    }

    TestdataSingleCascadingWithSupplyEntity setUpShadowVariables() {
        if (valueList != null && !valueList.isEmpty()) {
            int i = 0;
            var previous = valueList.get(i);
            var current = valueList.get(i);
            while (current != null) {
                current.setEntity(this);
                current.setPrevious(previous);
                previous = current;
                current = ++i < valueList.size() ? valueList.get(i) : null;
            }
            for (var v : valueList) {
                v.updateCascadeValue();
                v.updateCascadeValueWithReturnType();
            }
        }
        return this;
    }

    @PlanningListVariable(valueRangeProviderRefs = "valueRange")
    private List<TestdataSingleCascadingWithSupplyValue> valueList;

    public TestdataSingleCascadingWithSupplyEntity(String code) {
        super(code);
        this.valueList = new LinkedList<>();
    }

    public TestdataSingleCascadingWithSupplyEntity(String code, List<TestdataSingleCascadingWithSupplyValue> valueList) {
        super(code);
        this.valueList = valueList;
    }

    public void setValueList(List<TestdataSingleCascadingWithSupplyValue> valueList) {
        this.valueList = valueList;
    }

    public List<TestdataSingleCascadingWithSupplyValue> getValueList() {
        return valueList;
    }

    @Override
    public String toString() {
        return "TestdataCascadeEntity{" +
                "code='" + code + '\'' +
                '}';
    }
}
