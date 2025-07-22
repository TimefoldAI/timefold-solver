package ai.timefold.solver.core.testdomain.valuerange.entityproviding.composite;

import java.util.List;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.valuerange.ValueRangeProvider;
import ai.timefold.solver.core.api.domain.variable.PlanningVariable;
import ai.timefold.solver.core.impl.domain.entity.descriptor.EntityDescriptor;
import ai.timefold.solver.core.impl.domain.variable.descriptor.GenuineVariableDescriptor;
import ai.timefold.solver.core.testdomain.TestdataObject;
import ai.timefold.solver.core.testdomain.TestdataValue;

@PlanningEntity
public class TestdataCompositeEntityProvidingEntity extends TestdataObject {

    public static EntityDescriptor<TestdataCompositeEntityProvidingSolution> buildEntityDescriptor() {
        return TestdataCompositeEntityProvidingSolution.buildSolutionDescriptor()
                .findEntityDescriptorOrFail(TestdataCompositeEntityProvidingEntity.class);
    }

    public static GenuineVariableDescriptor<TestdataCompositeEntityProvidingSolution> buildVariableDescriptorForValue() {
        return buildEntityDescriptor().getGenuineVariableDescriptor("value");
    }

    private List<TestdataValue> valueRange1;
    private List<TestdataValue> valueRange2;

    private TestdataValue value;

    public TestdataCompositeEntityProvidingEntity() {
        // Required for cloning
    }

    public TestdataCompositeEntityProvidingEntity(String code, List<TestdataValue> valueRange1,
            List<TestdataValue> valueRange2) {
        super(code);
        this.valueRange1 = valueRange1;
        this.valueRange2 = valueRange2;
    }

    @PlanningVariable(valueRangeProviderRefs = { "valueRange1", "valueRange2" })
    public TestdataValue getValue() {
        return value;
    }

    public void setValue(TestdataValue value) {
        this.value = value;
    }

    @ValueRangeProvider(id = "valueRange1")
    public List<TestdataValue> getValueRange1() {
        return valueRange1;
    }

    public void setValueRange1(List<TestdataValue> valueRange1) {
        this.valueRange1 = valueRange1;
    }

    @ValueRangeProvider(id = "valueRange2")
    public List<TestdataValue> getValueRange2() {
        return valueRange2;
    }

    public void setValueRange2(List<TestdataValue> valueRange2) {
        this.valueRange2 = valueRange2;
    }
}
