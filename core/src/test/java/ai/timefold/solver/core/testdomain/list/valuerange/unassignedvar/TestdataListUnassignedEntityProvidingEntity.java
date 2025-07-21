package ai.timefold.solver.core.testdomain.list.valuerange.unassignedvar;

import java.util.ArrayList;
import java.util.List;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.valuerange.ValueRangeProvider;
import ai.timefold.solver.core.api.domain.variable.PlanningListVariable;
import ai.timefold.solver.core.impl.domain.entity.descriptor.EntityDescriptor;
import ai.timefold.solver.core.impl.domain.variable.descriptor.ListVariableDescriptor;
import ai.timefold.solver.core.testdomain.TestdataObject;
import ai.timefold.solver.core.testdomain.TestdataValue;

@PlanningEntity
public class TestdataListUnassignedEntityProvidingEntity extends TestdataObject {

    public static EntityDescriptor<TestdataListUnassignedEntityProvidingSolution> buildEntityDescriptor() {
        return TestdataListUnassignedEntityProvidingSolution.buildSolutionDescriptor()
                .findEntityDescriptorOrFail(TestdataListUnassignedEntityProvidingEntity.class);
    }

    public static ListVariableDescriptor<TestdataListUnassignedEntityProvidingSolution> buildVariableDescriptorForValueList() {
        return (ListVariableDescriptor<TestdataListUnassignedEntityProvidingSolution>) buildEntityDescriptor()
                .getGenuineVariableDescriptor("valueList");
    }

    @ValueRangeProvider(id = "valueRange")
    private final List<TestdataValue> valueRange;
    @PlanningListVariable(valueRangeProviderRefs = "valueRange", allowsUnassignedValues = true)
    private List<TestdataValue> valueList;

    public TestdataListUnassignedEntityProvidingEntity() {
        valueRange = new ArrayList<>();
        valueList = new ArrayList<>();
    }

    public TestdataListUnassignedEntityProvidingEntity(String code, List<TestdataValue> valueRange) {
        super(code);
        this.valueRange = valueRange;
        valueList = new ArrayList<>();
    }

    public List<TestdataValue> getValueRange() {
        return valueRange;
    }

    public List<TestdataValue> getValueList() {
        return valueList;
    }

    public void setValueList(List<TestdataValue> valueList) {
        this.valueList = valueList;
    }

}
