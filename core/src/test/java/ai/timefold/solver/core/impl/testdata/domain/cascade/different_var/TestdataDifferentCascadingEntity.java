package ai.timefold.solver.core.impl.testdata.domain.cascade.different_var;

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
public class TestdataDifferentCascadingEntity extends TestdataObject {

    public static EntityDescriptor<TestdataDifferentCascadingSolution> buildEntityDescriptor() {
        return TestdataDifferentCascadingSolution.buildSolutionDescriptor()
                .findEntityDescriptorOrFail(TestdataDifferentCascadingEntity.class);
    }

    public static ListVariableDescriptor<TestdataDifferentCascadingSolution> buildVariableDescriptorForValueList() {
        return (ListVariableDescriptor<TestdataDifferentCascadingSolution>) buildEntityDescriptor()
                .getGenuineVariableDescriptor("valueList");
    }

    public static TestdataDifferentCascadingEntity createWithValues(String code, TestdataDifferentCascadingValue... values) {
        // Set up shadow variables to preserve consistency.
        return new TestdataDifferentCascadingEntity(code, new ArrayList<>(Arrays.asList(values))).setUpShadowVariables();
    }

    TestdataDifferentCascadingEntity setUpShadowVariables() {
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
                v.updateSecondCascadeValue();
            }
        }
        return this;
    }

    @PlanningListVariable(valueRangeProviderRefs = "valueRange")
    private List<TestdataDifferentCascadingValue> valueList;

    public TestdataDifferentCascadingEntity() {
    }

    public TestdataDifferentCascadingEntity(String code) {
        super(code);
        this.valueList = new LinkedList<>();
    }

    public TestdataDifferentCascadingEntity(String code, List<TestdataDifferentCascadingValue> valueList) {
        super(code);
        this.valueList = valueList;
    }

    @SuppressWarnings("rawtypes")
    public void setValueList(List valueList) {
        this.valueList = valueList;
    }

    public List<TestdataDifferentCascadingValue> getValueList() {
        return valueList;
    }

    @Override
    public String toString() {
        return "TestdataDifferentCascadingEntity{" +
                "code='" + code + '\'' +
                '}';
    }
}
