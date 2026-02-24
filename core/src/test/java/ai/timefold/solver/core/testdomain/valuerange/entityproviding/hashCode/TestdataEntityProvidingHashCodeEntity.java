package ai.timefold.solver.core.testdomain.valuerange.entityproviding.hashCode;

import java.util.List;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.valuerange.ValueRangeProvider;
import ai.timefold.solver.core.api.domain.variable.PlanningVariable;
import ai.timefold.solver.core.impl.domain.entity.descriptor.EntityDescriptor;
import ai.timefold.solver.core.impl.domain.variable.descriptor.GenuineVariableDescriptor;
import ai.timefold.solver.core.testdomain.TestdataObject;
import ai.timefold.solver.core.testdomain.clone.lookup.TestdataObjectEquals;

@PlanningEntity
public class TestdataEntityProvidingHashCodeEntity extends TestdataObject {

    public static EntityDescriptor<TestdataEntityProvidingHashCodeSolution> buildEntityDescriptor() {
        return TestdataEntityProvidingHashCodeSolution.buildSolutionDescriptor()
                .findEntityDescriptorOrFail(TestdataEntityProvidingHashCodeEntity.class);
    }

    public static GenuineVariableDescriptor<TestdataEntityProvidingHashCodeSolution> buildVariableDescriptorForValue() {
        return buildEntityDescriptor().getGenuineVariableDescriptor("value");
    }

    private List<TestdataObjectEquals> valueRange;

    private TestdataObjectEquals value;

    public TestdataEntityProvidingHashCodeEntity() {
        // Required for cloning
    }

    public TestdataEntityProvidingHashCodeEntity(String code, List<TestdataObjectEquals> valueRange) {
        this(code, valueRange, null);
    }

    public TestdataEntityProvidingHashCodeEntity(String code, List<TestdataObjectEquals> valueRange,
            TestdataObjectEquals value) {
        super(code);
        this.valueRange = valueRange;
        this.value = value;
    }

    @PlanningVariable(valueRangeProviderRefs = "valueRange")
    public TestdataObjectEquals getValue() {
        return value;
    }

    public void setValue(TestdataObjectEquals value) {
        this.value = value;
    }

    @ValueRangeProvider(id = "valueRange")
    public List<TestdataObjectEquals> getValueRange() {
        return valueRange;
    }

    public void setValueRange(List<TestdataObjectEquals> valueRange) {
        this.valueRange = valueRange;
    }
}
