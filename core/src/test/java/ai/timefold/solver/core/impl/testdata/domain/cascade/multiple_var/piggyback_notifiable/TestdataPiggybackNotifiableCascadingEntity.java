package ai.timefold.solver.core.impl.testdata.domain.cascade.multiple_var.piggyback_notifiable;

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
public class TestdataPiggybackNotifiableCascadingEntity extends TestdataObject
        implements TestdataCascadingBaseEntity<TestdataPiggybackNotifiableCascadingValue> {

    public static EntityDescriptor<TestdataPiggybackNotifiableCascadingSolution> buildEntityDescriptor() {
        return TestdataPiggybackNotifiableCascadingSolution.buildSolutionDescriptor()
                .findEntityDescriptorOrFail(TestdataPiggybackNotifiableCascadingEntity.class);
    }

    public static ListVariableDescriptor<TestdataPiggybackNotifiableCascadingSolution> buildVariableDescriptorForValueList() {
        return (ListVariableDescriptor<TestdataPiggybackNotifiableCascadingSolution>) buildEntityDescriptor()
                .getGenuineVariableDescriptor("valueList");
    }

    public static TestdataPiggybackNotifiableCascadingEntity createWithValues(String code,
                                                                              TestdataPiggybackNotifiableCascadingValue... values) {
        // Set up shadow variables to preserve consistency.
        return new TestdataPiggybackNotifiableCascadingEntity(code, new ArrayList<>(Arrays.asList(values))).setUpShadowVariables();
    }

    TestdataPiggybackNotifiableCascadingEntity setUpShadowVariables() {
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
    private List<TestdataPiggybackNotifiableCascadingValue> valueList;

    public TestdataPiggybackNotifiableCascadingEntity() {
    }

    public TestdataPiggybackNotifiableCascadingEntity(String code) {
        super(code);
        this.valueList = new LinkedList<>();
    }

    public TestdataPiggybackNotifiableCascadingEntity(String code, List<TestdataPiggybackNotifiableCascadingValue> valueList) {
        super(code);
        this.valueList = valueList;
    }

    @Override
    @SuppressWarnings("rawtypes")
    public void setValueList(List valueList) {
        this.valueList = valueList;
    }

    @Override
    public List<TestdataPiggybackNotifiableCascadingValue> getValueList() {
        return valueList;
    }

    @Override
    public String toString() {
        return "TestdataMultipleCascadeEntity{" +
                "code='" + code + '\'' +
                '}';
    }
}
