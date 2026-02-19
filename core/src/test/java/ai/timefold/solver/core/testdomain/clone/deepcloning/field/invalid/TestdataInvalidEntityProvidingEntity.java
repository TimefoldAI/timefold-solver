package ai.timefold.solver.core.testdomain.clone.deepcloning.field.invalid;

import java.util.List;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.solution.cloner.DeepPlanningClone;
import ai.timefold.solver.core.api.domain.valuerange.ValueRangeProvider;
import ai.timefold.solver.core.api.domain.variable.PlanningVariable;
import ai.timefold.solver.core.impl.domain.entity.descriptor.EntityDescriptor;
import ai.timefold.solver.core.impl.domain.variable.descriptor.GenuineVariableDescriptor;
import ai.timefold.solver.core.testdomain.TestdataObject;
import ai.timefold.solver.core.testdomain.TestdataValue;

@PlanningEntity
public class TestdataInvalidEntityProvidingEntity extends TestdataObject {

    public static EntityDescriptor<TestdataInvalidEntityProvidingSolution> buildEntityDescriptor() {
        return TestdataInvalidEntityProvidingSolution.buildSolutionDescriptor()
                .findEntityDescriptorOrFail(TestdataInvalidEntityProvidingEntity.class);
    }

    public static GenuineVariableDescriptor<TestdataInvalidEntityProvidingSolution> buildVariableDescriptorForValue() {
        return buildEntityDescriptor().getGenuineVariableDescriptor("value");
    }

    @ValueRangeProvider(id = "valueRange")
    private List<TestdataValue> valueRange;

    @DeepPlanningClone
    private TestdataValue value; // TestdataValue is not deep-cloned, and the cloning logic should fail-fast

    public TestdataInvalidEntityProvidingEntity() {
        // Required for cloning
    }

    public TestdataInvalidEntityProvidingEntity(String code, List<TestdataValue> valueRange) {
        this(code, valueRange, null);
    }

    public TestdataInvalidEntityProvidingEntity(String code, List<TestdataValue> valueRange, TestdataValue value) {
        super(code);
        this.valueRange = valueRange;
        this.value = value;
    }

    @PlanningVariable(valueRangeProviderRefs = "valueRange")
    public TestdataValue getValue() {
        return value;
    }

    public void setValue(TestdataValue value) {
        this.value = value;
    }

    public List<TestdataValue> getValueRange() {
        return valueRange;
    }

    public void setValueRange(List<TestdataValue> valueRange) {
        this.valueRange = valueRange;
    }
}
