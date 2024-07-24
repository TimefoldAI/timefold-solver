package ai.timefold.solver.core.impl.testdata.domain.cascade.multiple_var.multiple_shadow_var;

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
public class TestdataMultipleSourceCascadingEntity extends TestdataObject
        implements TestdataCascadingBaseEntity<TestdataMultipleSourceCascadingValue> {

    public static EntityDescriptor<TestdataMultipleSourceCascadingSolution> buildEntityDescriptor() {
        return TestdataMultipleSourceCascadingSolution.buildSolutionDescriptor()
                .findEntityDescriptorOrFail(TestdataMultipleSourceCascadingEntity.class);
    }

    public static ListVariableDescriptor<TestdataMultipleSourceCascadingSolution> buildVariableDescriptorForValueList() {
        return (ListVariableDescriptor<TestdataMultipleSourceCascadingSolution>) buildEntityDescriptor()
                .getGenuineVariableDescriptor("valueList");
    }

    public static TestdataMultipleSourceCascadingEntity createWithValues(String code,
            TestdataMultipleSourceCascadingValue... values) {
        // Set up shadow variables to preserve consistency.
        return new TestdataMultipleSourceCascadingEntity(code, new ArrayList<>(Arrays.asList(values))).setUpShadowVariables();
    }

    TestdataMultipleSourceCascadingEntity setUpShadowVariables() {
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
    private List<TestdataMultipleSourceCascadingValue> valueList;

    public TestdataMultipleSourceCascadingEntity() {
    }

    public TestdataMultipleSourceCascadingEntity(String code) {
        super(code);
        this.valueList = new LinkedList<>();
    }

    public TestdataMultipleSourceCascadingEntity(String code, List<TestdataMultipleSourceCascadingValue> valueList) {
        super(code);
        this.valueList = valueList;
    }

    @Override
    @SuppressWarnings("rawtypes")
    public void setValueList(List valueList) {
        this.valueList = valueList;
    }

    @Override
    public List<TestdataMultipleSourceCascadingValue> getValueList() {
        return valueList;
    }

    @Override
    public String toString() {
        return "TestdataMultipleCascadeEntity{" +
                "code='" + code + '\'' +
                '}';
    }
}
