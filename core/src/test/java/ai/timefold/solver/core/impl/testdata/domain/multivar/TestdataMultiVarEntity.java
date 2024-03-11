package ai.timefold.solver.core.impl.testdata.domain.multivar;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.PlanningVariable;
import ai.timefold.solver.core.impl.domain.entity.descriptor.EntityDescriptor;
import ai.timefold.solver.core.impl.domain.variable.descriptor.GenuineVariableDescriptor;
import ai.timefold.solver.core.impl.testdata.domain.TestdataObject;
import ai.timefold.solver.core.impl.testdata.domain.TestdataValue;

@PlanningEntity
public class TestdataMultiVarEntity extends TestdataObject {

    public static EntityDescriptor<TestdataMultiVarSolution> buildEntityDescriptor() {
        return TestdataMultiVarSolution.buildSolutionDescriptor()
                .findEntityDescriptorOrFail(TestdataMultiVarEntity.class);
    }

    public static GenuineVariableDescriptor<TestdataMultiVarSolution> buildVariableDescriptorForPrimaryValue() {
        return buildEntityDescriptor().getGenuineVariableDescriptor("primaryValue");
    }

    public static GenuineVariableDescriptor<TestdataMultiVarSolution> buildVariableDescriptorForSecondaryValue() {
        return buildEntityDescriptor().getGenuineVariableDescriptor("secondaryValue");
    }

    private TestdataValue primaryValue;
    private TestdataValue secondaryValue;

    private TestdataOtherValue tertiaryValueAllowedUnassigned;

    public TestdataMultiVarEntity() {
    }

    public TestdataMultiVarEntity(String code) {
        super(code);
    }

    public TestdataMultiVarEntity(String code, TestdataValue primaryValue, TestdataValue secondaryValue,
            TestdataOtherValue tertiaryValueAllowedUnassigned) {
        super(code);
        this.primaryValue = primaryValue;
        this.secondaryValue = secondaryValue;
        this.tertiaryValueAllowedUnassigned = tertiaryValueAllowedUnassigned;
    }

    @PlanningVariable(valueRangeProviderRefs = "valueRange")
    public TestdataValue getPrimaryValue() {
        return primaryValue;
    }

    public void setPrimaryValue(TestdataValue primaryValue) {
        this.primaryValue = primaryValue;
    }

    @PlanningVariable(valueRangeProviderRefs = "valueRange")
    public TestdataValue getSecondaryValue() {
        return secondaryValue;
    }

    public void setSecondaryValue(TestdataValue secondaryValue) {
        this.secondaryValue = secondaryValue;
    }

    @PlanningVariable(valueRangeProviderRefs = "otherValueRange", allowsUnassigned = true)
    public TestdataOtherValue getTertiaryValueAllowedUnassigned() {
        return tertiaryValueAllowedUnassigned;
    }

    public void setTertiaryValueAllowedUnassigned(TestdataOtherValue tertiaryValueAllowedUnassigned) {
        this.tertiaryValueAllowedUnassigned = tertiaryValueAllowedUnassigned;
    }

    // ************************************************************************
    // Complex methods
    // ************************************************************************

}
