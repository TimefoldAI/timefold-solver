package ai.timefold.solver.core.testdomain.unassignedvar.composite;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.PlanningVariable;
import ai.timefold.solver.core.impl.domain.entity.descriptor.EntityDescriptor;
import ai.timefold.solver.core.impl.domain.variable.descriptor.GenuineVariableDescriptor;
import ai.timefold.solver.core.testdomain.TestdataObject;
import ai.timefold.solver.core.testdomain.TestdataValue;

@PlanningEntity
public class TestdataAllowsUnassignedCompositeEntity extends TestdataObject {

    public static EntityDescriptor<TestdataAllowsUnassignedCompositeSolution> buildEntityDescriptor() {
        return TestdataAllowsUnassignedCompositeSolution.buildSolutionDescriptor()
                .findEntityDescriptorOrFail(TestdataAllowsUnassignedCompositeEntity.class);
    }

    public static GenuineVariableDescriptor<TestdataAllowsUnassignedCompositeSolution> buildVariableDescriptorForValue() {
        return buildEntityDescriptor().getGenuineVariableDescriptor("value");
    }

    private TestdataValue value;

    public TestdataAllowsUnassignedCompositeEntity() {
        // Required for cloning
    }

    public TestdataAllowsUnassignedCompositeEntity(String code) {
        super(code);
    }

    public TestdataAllowsUnassignedCompositeEntity(String code, TestdataValue value) {
        this(code);
        this.value = value;
    }

    @PlanningVariable(valueRangeProviderRefs = { "valueRange1", "valueRange2" }, allowsUnassigned = true)
    public TestdataValue getValue() {
        return value;
    }

    public void setValue(TestdataValue value) {
        this.value = value;
    }

}
