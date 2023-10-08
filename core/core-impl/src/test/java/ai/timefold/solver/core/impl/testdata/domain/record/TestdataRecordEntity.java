package ai.timefold.solver.core.impl.testdata.domain.record;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.PlanningVariable;
import ai.timefold.solver.core.impl.domain.entity.descriptor.EntityDescriptor;
import ai.timefold.solver.core.impl.domain.variable.descriptor.GenuineVariableDescriptor;
import ai.timefold.solver.core.impl.testdata.domain.TestdataObject;

@PlanningEntity
public class TestdataRecordEntity extends TestdataObject {

    public static EntityDescriptor<TestdataRecordSolution> buildEntityDescriptor() {
        return TestdataRecordSolution.buildSolutionDescriptor().findEntityDescriptorOrFail(TestdataRecordEntity.class);
    }

    public static GenuineVariableDescriptor<TestdataRecordSolution> buildVariableDescriptorForValue() {
        return buildEntityDescriptor().getGenuineVariableDescriptor("value");
    }

    private TestdataRecordValue value;

    public TestdataRecordEntity() {
    }

    public TestdataRecordEntity(String code) {
        super(code);
    }

    public TestdataRecordEntity(String code, TestdataRecordValue value) {
        this(code);
        this.value = value;
    }

    @PlanningVariable(valueRangeProviderRefs = "valueRange")
    public TestdataRecordValue getValue() {
        return value;
    }

    public void setValue(TestdataRecordValue value) {
        this.value = value;
    }

    // ************************************************************************
    // Complex methods
    // ************************************************************************

}
