package ai.timefold.solver.core.testdomain.list.valuerange;

import java.util.ArrayList;
import java.util.List;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.valuerange.ValueRangeProvider;
import ai.timefold.solver.core.api.domain.variable.PlanningListVariable;
import ai.timefold.solver.core.impl.domain.entity.descriptor.EntityDescriptor;
import ai.timefold.solver.core.impl.domain.variable.descriptor.ListVariableDescriptor;
import ai.timefold.solver.core.testdomain.TestdataObject;
import ai.timefold.solver.core.testdomain.list.TestdataListValue;

@PlanningEntity
public class TestdataListEntityProvidingEntity extends TestdataObject {

    public static EntityDescriptor<TestdataListEntityProvidingSolution> buildEntityDescriptor() {
        return TestdataListEntityProvidingSolution.buildSolutionDescriptor()
                .findEntityDescriptorOrFail(TestdataListEntityProvidingEntity.class);
    }

    public static ListVariableDescriptor<TestdataListEntityProvidingSolution> buildVariableDescriptorForValueList() {
        return (ListVariableDescriptor<TestdataListEntityProvidingSolution>) buildEntityDescriptor()
                .getGenuineVariableDescriptor("valueList");
    }

    @ValueRangeProvider(id = "valueRange")
    private final List<TestdataListValue> valueRange;
    @PlanningListVariable(valueRangeProviderRefs = "valueRange")
    private List<TestdataListValue> valueList;

    public TestdataListEntityProvidingEntity() {
        // Required for cloning
        valueRange = new ArrayList<>();
        valueList = new ArrayList<>();
    }

    public TestdataListEntityProvidingEntity(String code, List<TestdataListValue> valueRange) {
        super(code);
        this.valueRange = valueRange;
        valueList = new ArrayList<>();
    }

    public List<TestdataListValue> getValueRange() {
        return valueRange;
    }

    public List<TestdataListValue> getValueList() {
        return valueList;
    }

    public void setValueList(List<TestdataListValue> valueList) {
        this.valueList = valueList;
    }
}
