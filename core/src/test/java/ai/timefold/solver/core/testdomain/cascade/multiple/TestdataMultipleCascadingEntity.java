package ai.timefold.solver.core.testdomain.cascade.multiple;

import java.util.LinkedList;
import java.util.List;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.PlanningListVariable;
import ai.timefold.solver.core.impl.domain.entity.descriptor.EntityDescriptor;
import ai.timefold.solver.core.impl.domain.variable.descriptor.ListVariableDescriptor;
import ai.timefold.solver.core.testdomain.TestdataObject;

@PlanningEntity
public class TestdataMultipleCascadingEntity extends TestdataObject {

    public static EntityDescriptor<TestdataMultipleCascadingSolution> buildEntityDescriptor() {
        return TestdataMultipleCascadingSolution.buildSolutionDescriptor()
                .findEntityDescriptorOrFail(TestdataMultipleCascadingEntity.class);
    }

    public static ListVariableDescriptor<TestdataMultipleCascadingSolution> buildVariableDescriptorForValueList() {
        return (ListVariableDescriptor<TestdataMultipleCascadingSolution>) buildEntityDescriptor()
                .getGenuineVariableDescriptor("valueList");
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

    @SuppressWarnings("rawtypes")
    public void setValueList(List valueList) {
        this.valueList = valueList;
    }

    public List<TestdataMultipleCascadingValue> getValueList() {
        return valueList;
    }

    @Override
    public String toString() {
        return "TestdataMultipleCascadingEntity{" +
                "code='" + code + '\'' +
                '}';
    }
}
