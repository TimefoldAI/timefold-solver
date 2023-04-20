package ai.timefold.solver.core.impl.testdata.domain.list.mixed;

import java.util.List;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.PlanningListVariable;
import ai.timefold.solver.core.api.domain.variable.PlanningVariable;
import ai.timefold.solver.core.impl.domain.entity.descriptor.EntityDescriptor;
import ai.timefold.solver.core.impl.domain.variable.descriptor.ListVariableDescriptor;
import ai.timefold.solver.core.impl.testdata.domain.TestdataObject;
import ai.timefold.solver.core.impl.testdata.domain.TestdataValue;

@PlanningEntity
public class TestdataMixedVariablesEntity extends TestdataObject {

    public static EntityDescriptor<TestdataMixedVariablesSolution> buildEntityDescriptor() {
        return TestdataMixedVariablesSolution.buildSolutionDescriptor()
                .findEntityDescriptorOrFail(TestdataMixedVariablesEntity.class);
    }

    public static ListVariableDescriptor<TestdataMixedVariablesSolution> buildVariableDescriptorForValueList() {
        return (ListVariableDescriptor<TestdataMixedVariablesSolution>) buildEntityDescriptor()
                .getGenuineVariableDescriptor("valueList");
    }

    @PlanningListVariable(valueRangeProviderRefs = "valueRange")
    private final List<TestdataValue> valueList;
    @PlanningVariable(valueRangeProviderRefs = "valueRange")
    private TestdataValue value;

    public TestdataMixedVariablesEntity(String code, List<TestdataValue> valueList, TestdataValue value) {
        super(code);
        this.valueList = valueList;
        this.value = value;
    }

    public List<TestdataValue> getValueList() {
        return valueList;
    }

    public TestdataValue getValue() {
        return value;
    }
}
