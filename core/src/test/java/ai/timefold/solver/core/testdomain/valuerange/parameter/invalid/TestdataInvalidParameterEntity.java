package ai.timefold.solver.core.testdomain.valuerange.parameter.invalid;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.PlanningVariable;
import ai.timefold.solver.core.impl.domain.entity.descriptor.EntityDescriptor;
import ai.timefold.solver.core.testdomain.TestdataObject;
import ai.timefold.solver.core.testdomain.TestdataValue;
import ai.timefold.solver.core.testdomain.valuerange.entityproviding.parameter.invalid.TestdataInvalidCountEntityProvidingWithParameterSolution;

@PlanningEntity
public class TestdataInvalidParameterEntity extends TestdataObject {

    public static EntityDescriptor<TestdataInvalidCountEntityProvidingWithParameterSolution> buildEntityDescriptor() {
        return TestdataInvalidCountEntityProvidingWithParameterSolution.buildSolutionDescriptor()
                .findEntityDescriptorOrFail(TestdataInvalidParameterEntity.class);
    }

    private TestdataValue value;

    public TestdataInvalidParameterEntity() {
        // Required for cloning
    }

    public TestdataInvalidParameterEntity(String code) {
        this(code, null);
    }

    public TestdataInvalidParameterEntity(String code, TestdataValue value) {
        super(code);
        this.value = value;
    }

    @PlanningVariable(valueRangeProviderRefs = "valueRange")
    public TestdataValue getValue() {
        return value;
    }

    public void setValue(TestdataValue value) {
        this.value = value;
    }

}
