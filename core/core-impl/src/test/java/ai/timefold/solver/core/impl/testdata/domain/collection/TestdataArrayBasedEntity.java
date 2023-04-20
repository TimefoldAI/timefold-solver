package ai.timefold.solver.core.impl.testdata.domain.collection;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.PlanningVariable;
import ai.timefold.solver.core.impl.domain.entity.descriptor.EntityDescriptor;
import ai.timefold.solver.core.impl.testdata.domain.TestdataObject;
import ai.timefold.solver.core.impl.testdata.domain.TestdataValue;

@PlanningEntity
public class TestdataArrayBasedEntity extends TestdataObject {

    public static EntityDescriptor<TestdataArrayBasedSolution> buildEntityDescriptor() {
        return TestdataArrayBasedSolution.buildSolutionDescriptor()
                .findEntityDescriptorOrFail(TestdataArrayBasedEntity.class);
    }

    private TestdataArrayBasedEntity[] entities;

    private TestdataValue value;

    public TestdataArrayBasedEntity() {
    }

    public TestdataArrayBasedEntity(String code) {
        super(code);
    }

    public TestdataArrayBasedEntity(String code, TestdataValue value) {
        this(code);
        this.value = value;
    }

    public TestdataArrayBasedEntity[] getEntities() {
        return entities;
    }

    public void setEntities(TestdataArrayBasedEntity[] entities) {
        this.entities = entities;
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
