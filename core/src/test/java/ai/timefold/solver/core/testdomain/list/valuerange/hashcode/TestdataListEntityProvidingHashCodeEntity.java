package ai.timefold.solver.core.testdomain.list.valuerange.hashcode;

import java.util.ArrayList;
import java.util.List;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.valuerange.ValueRangeProvider;
import ai.timefold.solver.core.api.domain.variable.PlanningListVariable;
import ai.timefold.solver.core.impl.domain.entity.descriptor.EntityDescriptor;
import ai.timefold.solver.core.impl.domain.variable.descriptor.ListVariableDescriptor;
import ai.timefold.solver.core.testdomain.TestdataObject;
import ai.timefold.solver.core.testdomain.clone.lookup.TestdataObjectEquals;

@PlanningEntity
public class TestdataListEntityProvidingHashCodeEntity extends TestdataObject {

    public static EntityDescriptor<TestdataListEntityProvidingHashCodeSolution> buildEntityDescriptor() {
        return TestdataListEntityProvidingHashCodeSolution.buildSolutionDescriptor()
                .findEntityDescriptorOrFail(TestdataListEntityProvidingHashCodeEntity.class);
    }

    public static ListVariableDescriptor<TestdataListEntityProvidingHashCodeSolution> buildVariableDescriptorForValueList() {
        return (ListVariableDescriptor<TestdataListEntityProvidingHashCodeSolution>) buildEntityDescriptor()
                .getGenuineVariableDescriptor("valueList");
    }

    @ValueRangeProvider(id = "valueRange")
    private final List<TestdataObjectEquals> valueRange;
    @PlanningListVariable(valueRangeProviderRefs = "valueRange")
    private List<TestdataObjectEquals> valueList;

    public TestdataListEntityProvidingHashCodeEntity() {
        // Required for cloning
        valueRange = new ArrayList<>();
        valueList = new ArrayList<>();
    }

    public TestdataListEntityProvidingHashCodeEntity(String code, List<TestdataObjectEquals> valueRange) {
        super(code);
        this.valueRange = valueRange;
        valueList = new ArrayList<>();
    }

    public TestdataListEntityProvidingHashCodeEntity(String code, List<TestdataObjectEquals> valueRange,
            List<TestdataObjectEquals> valueList) {
        super(code);
        this.valueRange = valueRange;
        this.valueList = valueList;
    }

    public List<TestdataObjectEquals> getValueRange() {
        return valueRange;
    }

    public List<TestdataObjectEquals> getValueList() {
        return valueList;
    }

    public void setValueList(List<TestdataObjectEquals> valueList) {
        this.valueList = valueList;
    }
}
