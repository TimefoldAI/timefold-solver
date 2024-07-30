package ai.timefold.solver.core.impl.testdata.domain.cascade.multiple_var.piggyback;

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
public class TestdataPiggybackCascadingEntity extends TestdataObject
        implements TestdataCascadingBaseEntity<TestdataPiggybackCascadingValue> {

    public static EntityDescriptor<TestdataPiggybackCascadingSolution> buildEntityDescriptor() {
        return TestdataPiggybackCascadingSolution.buildSolutionDescriptor()
                .findEntityDescriptorOrFail(TestdataPiggybackCascadingEntity.class);
    }

    public static ListVariableDescriptor<TestdataPiggybackCascadingSolution> buildVariableDescriptorForValueList() {
        return (ListVariableDescriptor<TestdataPiggybackCascadingSolution>) buildEntityDescriptor()
                .getGenuineVariableDescriptor("valueList");
    }

    public static TestdataPiggybackCascadingEntity createWithValues(String code,
            TestdataPiggybackCascadingValue... values) {
        // Set up shadow variables to preserve consistency.
        return new TestdataPiggybackCascadingEntity(code, new ArrayList<>(Arrays.asList(values))).setUpShadowVariables();
    }

    TestdataPiggybackCascadingEntity setUpShadowVariables() {
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
    private List<TestdataPiggybackCascadingValue> valueList;

    public TestdataPiggybackCascadingEntity() {
    }

    public TestdataPiggybackCascadingEntity(String code) {
        super(code);
        this.valueList = new LinkedList<>();
    }

    public TestdataPiggybackCascadingEntity(String code, List<TestdataPiggybackCascadingValue> valueList) {
        super(code);
        this.valueList = valueList;
    }

    @Override
    @SuppressWarnings("rawtypes")
    public void setValueList(List valueList) {
        this.valueList = valueList;
    }

    @Override
    public List<TestdataPiggybackCascadingValue> getValueList() {
        return valueList;
    }

    @Override
    public String toString() {
        return "TestdataPiggybackCascadingEntity{" +
                "code='" + code + '\'' +
                '}';
    }
}
