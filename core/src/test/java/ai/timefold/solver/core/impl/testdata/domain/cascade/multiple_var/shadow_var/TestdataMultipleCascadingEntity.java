package ai.timefold.solver.core.impl.testdata.domain.cascade.multiple_var.shadow_var;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.PlanningListVariable;
import ai.timefold.solver.core.impl.domain.entity.descriptor.EntityDescriptor;
import ai.timefold.solver.core.impl.domain.variable.descriptor.ListVariableDescriptor;
import ai.timefold.solver.core.impl.testdata.domain.TestdataObject;
import ai.timefold.solver.core.impl.testdata.domain.cascade.multiple_var.TestdataCascadingBaseEntity;

@PlanningEntity
public class TestdataMultipleCascadingEntity extends TestdataObject
        implements TestdataCascadingBaseEntity<TestdataMultipleCascadingValue> {

    public static EntityDescriptor<TestdataMultipleCascadingSolution> buildEntityDescriptor() {
        return TestdataMultipleCascadingSolution.buildSolutionDescriptor()
                .findEntityDescriptorOrFail(TestdataMultipleCascadingEntity.class);
    }

    public static ListVariableDescriptor<TestdataMultipleCascadingSolution> buildVariableDescriptorForValueList() {
        return (ListVariableDescriptor<TestdataMultipleCascadingSolution>) buildEntityDescriptor()
                .getGenuineVariableDescriptor("valueList");
    }

    public static TestdataMultipleCascadingEntity createWithValues(String code, TestdataMultipleCascadingValue... values) {
        // Set up shadow variables to preserve consistency.
        return new TestdataMultipleCascadingEntity(code, new ArrayList<>(Arrays.asList(values))).setUpShadowVariables();
    }

    TestdataMultipleCascadingEntity setUpShadowVariables() {
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
    private List<TestdataMultipleCascadingValue> valueList;

    public TestdataMultipleCascadingEntity() {
    }

    public TestdataMultipleCascadingEntity(String code) {
        super(code);
        this.valueList = new LinkedList<>();
    }

    public TestdataMultipleCascadingEntity(String code, List<TestdataMultipleCascadingValue> valueList) {
        super(code);
        this.valueList = valueList;
    }

    @Override
    @SuppressWarnings("rawtypes")
    public void setValueList(List valueList) {
        this.valueList = valueList;
    }

    @Override
    public List<TestdataMultipleCascadingValue> getValueList() {
        return valueList;
    }

    @Override
    public String toString() {
        return "TestdataMultipleCascadeEntity{" +
                "code='" + code + '\'' +
                '}';
    }
}
