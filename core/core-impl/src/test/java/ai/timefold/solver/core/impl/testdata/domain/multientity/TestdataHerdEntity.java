package ai.timefold.solver.core.impl.testdata.domain.multientity;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.PlanningVariable;
import ai.timefold.solver.core.impl.domain.entity.descriptor.EntityDescriptor;
import ai.timefold.solver.core.impl.testdata.domain.TestdataObject;

@PlanningEntity
public class TestdataHerdEntity extends TestdataObject {

    public static EntityDescriptor<TestdataMultiEntitySolution> buildEntityDescriptor() {
        return TestdataMultiEntitySolution.buildSolutionDescriptor()
                .findEntityDescriptorOrFail(TestdataHerdEntity.class);
    }

    private TestdataLeadEntity leadEntity;

    public TestdataHerdEntity() {
    }

    public TestdataHerdEntity(String code) {
        super(code);
    }

    public TestdataHerdEntity(String code, TestdataLeadEntity leadEntity) {
        super(code);
        this.leadEntity = leadEntity;
    }

    @PlanningVariable(valueRangeProviderRefs = "leadEntityRange")
    public TestdataLeadEntity getLeadEntity() {
        return leadEntity;
    }

    public void setLeadEntity(TestdataLeadEntity leadEntity) {
        this.leadEntity = leadEntity;
    }

    // ************************************************************************
    // Complex methods
    // ************************************************************************

}
