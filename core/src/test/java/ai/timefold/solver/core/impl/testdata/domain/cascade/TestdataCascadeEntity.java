package ai.timefold.solver.core.impl.testdata.domain.cascade;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.PlanningListVariable;
import ai.timefold.solver.core.impl.domain.entity.descriptor.EntityDescriptor;
import ai.timefold.solver.core.impl.domain.variable.descriptor.ListVariableDescriptor;
import ai.timefold.solver.core.impl.testdata.domain.TestdataObject;

@PlanningEntity
public class TestdataCascadeEntity extends TestdataObject {

    public static EntityDescriptor<TestdataCascadeSolution> buildEntityDescriptor() {
        return TestdataCascadeSolution.buildSolutionDescriptor().findEntityDescriptorOrFail(TestdataCascadeEntity.class);
    }

    public static ListVariableDescriptor<TestdataCascadeSolution> buildVariableDescriptorForValueList() {
        return (ListVariableDescriptor<TestdataCascadeSolution>) buildEntityDescriptor()
                .getGenuineVariableDescriptor("valueList");
    }

    public static TestdataCascadeEntity createWithValues(String code, TestdataCascadeValue... values) {
        // Set up shadow variables to preserve consistency.
        return new TestdataCascadeEntity(code, new ArrayList<>(Arrays.asList(values))).setUpShadowVariables();
    }

    TestdataCascadeEntity setUpShadowVariables() {
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
    private List<TestdataCascadeValue> valueList;

    public TestdataCascadeEntity() {
    }

    public TestdataCascadeEntity(String code) {
        super(code);
        this.valueList = valueList;
    }

    public TestdataCascadeEntity(String code, List<TestdataCascadeValue> valueList) {
        super(code);
        this.valueList = valueList;
    }

    public void setValueList(List<TestdataCascadeValue> valueList) {
        this.valueList = valueList;
    }

    public List<TestdataCascadeValue> getValueList() {
        return valueList;
    }
}
