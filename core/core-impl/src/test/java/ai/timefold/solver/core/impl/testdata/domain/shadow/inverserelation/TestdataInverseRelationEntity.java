package ai.timefold.solver.core.impl.testdata.domain.shadow.inverserelation;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.PlanningVariable;
import ai.timefold.solver.core.impl.domain.entity.descriptor.EntityDescriptor;
import ai.timefold.solver.core.impl.domain.variable.descriptor.GenuineVariableDescriptor;
import ai.timefold.solver.core.impl.testdata.domain.TestdataObject;

@PlanningEntity
public class TestdataInverseRelationEntity extends TestdataObject {

    public static EntityDescriptor<TestdataInverseRelationSolution> buildEntityDescriptor() {
        return TestdataInverseRelationSolution.buildSolutionDescriptor()
                .findEntityDescriptorOrFail(TestdataInverseRelationEntity.class);
    }

    public static GenuineVariableDescriptor<TestdataInverseRelationSolution> buildVariableDescriptorForValue() {
        return buildEntityDescriptor().getGenuineVariableDescriptor("value");
    }

    private TestdataInverseRelationValue value;

    public TestdataInverseRelationEntity() {
    }

    public TestdataInverseRelationEntity(String code) {
        super(code);
    }

    public TestdataInverseRelationEntity(String code, TestdataInverseRelationValue value) {
        this(code);
        this.value = value;
        if (value != null) {
            value.getEntities().add(this);
        }
    }

    @PlanningVariable(valueRangeProviderRefs = "valueRange", allowsUnassigned = true)
    public TestdataInverseRelationValue getValue() {
        return value;
    }

    public void setValue(TestdataInverseRelationValue value) {
        this.value = value;
    }

    // ************************************************************************
    // Complex methods
    // ************************************************************************

}
