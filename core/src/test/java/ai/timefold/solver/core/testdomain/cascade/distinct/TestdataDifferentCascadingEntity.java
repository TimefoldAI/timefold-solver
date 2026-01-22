package ai.timefold.solver.core.testdomain.cascade.distinct;

import java.util.LinkedList;
import java.util.List;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.PlanningListVariable;
import ai.timefold.solver.core.impl.domain.entity.descriptor.EntityDescriptor;
import ai.timefold.solver.core.impl.domain.variable.descriptor.ListVariableDescriptor;
import ai.timefold.solver.core.testdomain.TestdataObject;

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
