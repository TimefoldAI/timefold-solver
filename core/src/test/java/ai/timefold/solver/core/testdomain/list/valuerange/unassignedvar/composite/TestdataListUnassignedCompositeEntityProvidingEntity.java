package ai.timefold.solver.core.testdomain.list.valuerange.unassignedvar.composite;

import java.util.ArrayList;
import java.util.List;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.valuerange.ValueRangeProvider;
import ai.timefold.solver.core.api.domain.variable.PlanningListVariable;
import ai.timefold.solver.core.impl.domain.entity.descriptor.EntityDescriptor;
import ai.timefold.solver.core.impl.domain.variable.descriptor.ListVariableDescriptor;
import ai.timefold.solver.core.testdomain.TestdataObject;
import ai.timefold.solver.core.testdomain.list.valuerange.TestdataListEntityProvidingValue;

@PlanningEntity
public class TestdataListUnassignedCompositeEntityProvidingEntity extends TestdataObject {

    public static EntityDescriptor<TestdataListUnassignedCompositeEntityProvidingSolution> buildEntityDescriptor() {
        return TestdataListUnassignedCompositeEntityProvidingSolution.buildSolutionDescriptor()
                .findEntityDescriptorOrFail(TestdataListUnassignedCompositeEntityProvidingEntity.class);
    }

    public static ListVariableDescriptor<TestdataListUnassignedCompositeEntityProvidingSolution>
            buildVariableDescriptorForValueList() {
        return (ListVariableDescriptor<TestdataListUnassignedCompositeEntityProvidingSolution>) buildEntityDescriptor()
                .getGenuineVariableDescriptor("valueList");
    }

    @ValueRangeProvider(id = "valueRange1")
    private final List<TestdataListEntityProvidingValue> valueRange1;
    @ValueRangeProvider(id = "valueRange2")
    private final List<TestdataListEntityProvidingValue> valueRange2;
    @PlanningListVariable(valueRangeProviderRefs = { "valueRange1", "valueRange2" }, allowsUnassignedValues = true)
    private List<TestdataListEntityProvidingValue> valueList;

    public TestdataListUnassignedCompositeEntityProvidingEntity() {
        valueList = new ArrayList<>();
        valueRange1 = new ArrayList<>();
        valueRange2 = new ArrayList<>();
    }

    public TestdataListUnassignedCompositeEntityProvidingEntity(String code, List<TestdataListEntityProvidingValue> valueRange1,
            List<TestdataListEntityProvidingValue> valueRange2) {
        super(code);
        this.valueRange1 = valueRange1;
        this.valueRange2 = valueRange2;
        valueList = new ArrayList<>();
    }

    public List<TestdataListEntityProvidingValue> getValueRange1() {
        return valueRange1;
    }

    public List<TestdataListEntityProvidingValue> getValueList() {
        return valueList;
    }

    public void setValueList(List<TestdataListEntityProvidingValue> valueList) {
        this.valueList = valueList;
    }
}
