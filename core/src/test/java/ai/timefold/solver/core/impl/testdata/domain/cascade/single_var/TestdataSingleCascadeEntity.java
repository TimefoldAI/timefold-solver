package ai.timefold.solver.core.impl.testdata.domain.cascade.single_var;

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
public class TestdataSingleCascadeEntity extends TestdataObject {

    public static EntityDescriptor<TestdataSingleCascadeSolution> buildEntityDescriptor() {
        return TestdataSingleCascadeSolution.buildSolutionDescriptor().findEntityDescriptorOrFail(TestdataSingleCascadeEntity.class);
    }

    public static ListVariableDescriptor<TestdataSingleCascadeSolution> buildVariableDescriptorForValueList() {
        return (ListVariableDescriptor<TestdataSingleCascadeSolution>) buildEntityDescriptor()
                .getGenuineVariableDescriptor("valueList");
    }

    public static TestdataSingleCascadeEntity createWithValues(String code, TestdataSingleCascadeValue... values) {
        // Set up shadow variables to preserve consistency.
        return new TestdataSingleCascadeEntity(code, new ArrayList<>(Arrays.asList(values))).setUpShadowVariables();
    }

    TestdataSingleCascadeEntity setUpShadowVariables() {
        if (valueList != null && !valueList.isEmpty()) {
            int i = 0;
            var previous = valueList.get(i);
            var current = valueList.get(i);
            while (current != null) {
                current.setEntity(this);
                current.setPrevious(previous);
                if (previous != null) {
                    previous.setNext(current);
                }
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
    private List<TestdataSingleCascadeValue> valueList;

    public TestdataSingleCascadeEntity(String code) {
        super(code);
        this.valueList = new LinkedList<>();
    }

    public TestdataSingleCascadeEntity(String code, List<TestdataSingleCascadeValue> valueList) {
        super(code);
        this.valueList = valueList;
    }

    public void setValueList(List<TestdataSingleCascadeValue> valueList) {
        this.valueList = valueList;
    }

    public List<TestdataSingleCascadeValue> getValueList() {
        return valueList;
    }

    @Override
    public String toString() {
        return "TestdataCascadeEntity{" +
                "code='" + code + '\'' +
                '}';
    }
}
