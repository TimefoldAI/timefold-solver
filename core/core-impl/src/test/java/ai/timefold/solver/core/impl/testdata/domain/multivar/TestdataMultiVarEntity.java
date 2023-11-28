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

    public static GenuineVariableDescriptor<TestdataMultiVarSolution> buildVariableDescriptorForTertiaryNullableValue() {
        return buildEntityDescriptor().getGenuineVariableDescriptor("tertiaryNullableValue");
    }

    private TestdataValue primaryValue;
    private TestdataValue secondaryValue;

    private TestdataOtherValue tertiaryNullableValue;

    public TestdataMultiVarEntity() {
    }

    public TestdataMultiVarEntity(String code) {
        super(code);
    }

    public TestdataMultiVarEntity(String code, TestdataValue primaryValue, TestdataValue secondaryValue,
            TestdataOtherValue tertiaryNullableValue) {
        super(code);
        this.primaryValue = primaryValue;
        this.secondaryValue = secondaryValue;
        this.tertiaryNullableValue = tertiaryNullableValue;
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

    @PlanningVariable(valueRangeProviderRefs = "otherValueRange", nullable = true)
    public TestdataOtherValue getTertiaryNullableValue() {
        return tertiaryNullableValue;
    }

    public void setTertiaryNullableValue(TestdataOtherValue tertiaryNullableValue) {
        this.tertiaryNullableValue = tertiaryNullableValue;
    }

    // ************************************************************************
    // Complex methods
    // ************************************************************************

}
