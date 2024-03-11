package ai.timefold.solver.core.impl.testdata.domain.collection;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.PlanningVariable;
import ai.timefold.solver.core.impl.domain.entity.descriptor.EntityDescriptor;
import ai.timefold.solver.core.impl.testdata.domain.TestdataObject;
import ai.timefold.solver.core.impl.testdata.domain.TestdataValue;

@PlanningEntity
public class TestdataSetBasedEntity extends TestdataObject {

    public static EntityDescriptor<TestdataSetBasedSolution> buildEntityDescriptor() {
        return TestdataSetBasedSolution.buildSolutionDescriptor()
                .findEntityDescriptorOrFail(TestdataSetBasedEntity.class);
    }

    private TestdataValue value;

    public TestdataSetBasedEntity() {
    }

    public TestdataSetBasedEntity(String code) {
        super(code);
    }

    public TestdataSetBasedEntity(String code, TestdataValue value) {
        this(code);
        this.value = value;
    }

    @PlanningVariable(valueRangeProviderRefs = "valueRange")
    public TestdataValue getValue() {
        return value;
    }

    public void setValue(TestdataValue value) {
        this.value = value;
    }

    // ************************************************************************
    // Complex methods
    // ************************************************************************

}
