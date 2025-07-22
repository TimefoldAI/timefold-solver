package ai.timefold.solver.core.testdomain.composite;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.PlanningVariable;
import ai.timefold.solver.core.impl.domain.entity.descriptor.EntityDescriptor;
import ai.timefold.solver.core.impl.domain.variable.descriptor.GenuineVariableDescriptor;
import ai.timefold.solver.core.testdomain.TestdataObject;
import ai.timefold.solver.core.testdomain.TestdataValue;

@PlanningEntity
public class TestdataCompositeEntity extends TestdataObject {

    public static EntityDescriptor<TestdataCompositeSolution> buildEntityDescriptor() {
        return TestdataCompositeSolution.buildSolutionDescriptor().findEntityDescriptorOrFail(TestdataCompositeEntity.class);
    }

    public static GenuineVariableDescriptor<TestdataCompositeSolution> buildVariableDescriptorForValue() {
        return buildEntityDescriptor().getGenuineVariableDescriptor("value");
    }

    private TestdataValue value;

    public TestdataCompositeEntity() {
        // Required for cloning
    }

    public TestdataCompositeEntity(String code) {
        super(code);
    }

    public TestdataCompositeEntity(String code, TestdataValue value) {
        this(code);
        this.value = value;
    }

    @PlanningVariable(valueRangeProviderRefs = { "valueRange1", "valueRange2" })
    public TestdataValue getValue() {
        return value;
    }

    public void setValue(TestdataValue value) {
        this.value = value;
    }

}
