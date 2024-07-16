package ai.timefold.solver.core.impl.testdata.domain.cascade.multiple_var;

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
public class TestdataMultipleCascadeEntity extends TestdataObject {

    public static EntityDescriptor<TestdataMultipleCascadeSolution> buildEntityDescriptor() {
        return TestdataMultipleCascadeSolution.buildSolutionDescriptor().findEntityDescriptorOrFail(TestdataMultipleCascadeEntity.class);
    }

    public static ListVariableDescriptor<TestdataMultipleCascadeSolution> buildVariableDescriptorForValueList() {
        return (ListVariableDescriptor<TestdataMultipleCascadeSolution>) buildEntityDescriptor()
                .getGenuineVariableDescriptor("valueList");
    }

    public static TestdataMultipleCascadeEntity createWithValues(String code, TestdataMultipleCascadeValue... values) {
        // Set up shadow variables to preserve consistency.
        return new TestdataMultipleCascadeEntity(code, new ArrayList<>(Arrays.asList(values))).setUpShadowVariables();
    }

    TestdataMultipleCascadeEntity setUpShadowVariables() {
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
            }
        }
        return this;
    }

    @PlanningListVariable(valueRangeProviderRefs = "valueRange")
    private List<TestdataMultipleCascadeValue> valueList;

    public TestdataMultipleCascadeEntity() {
    }

    public TestdataMultipleCascadeEntity(String code) {
        super(code);
        this.valueList = new LinkedList<>();
    }

    public TestdataMultipleCascadeEntity(String code, List<TestdataMultipleCascadeValue> valueList) {
        super(code);
        this.valueList = valueList;
    }

    public void setValueList(List<TestdataMultipleCascadeValue> valueList) {
        this.valueList = valueList;
    }

    public List<TestdataMultipleCascadeValue> getValueList() {
        return valueList;
    }

    @Override
    public String toString() {
        return "TestdataMultipleCascadeEntity{" +
                "code='" + code + '\'' +
                '}';
    }
}
