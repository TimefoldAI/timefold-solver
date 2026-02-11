package ai.timefold.solver.core.testdomain.shadow;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.PlanningVariable;
import ai.timefold.solver.core.api.domain.variable.ShadowSources;
import ai.timefold.solver.core.api.domain.variable.ShadowVariable;
import ai.timefold.solver.core.impl.domain.entity.descriptor.EntityDescriptor;
import ai.timefold.solver.core.impl.domain.variable.descriptor.GenuineVariableDescriptor;
import ai.timefold.solver.core.testdomain.TestdataObject;
import ai.timefold.solver.core.testdomain.TestdataValue;

@PlanningEntity
public class TestdataShadowedEntity extends TestdataObject {

    public static EntityDescriptor<TestdataShadowedSolution> buildEntityDescriptor() {
        return TestdataShadowedSolution.buildSolutionDescriptor()
                .findEntityDescriptorOrFail(TestdataShadowedEntity.class);
    }

    public static GenuineVariableDescriptor<TestdataShadowedSolution> buildVariableDescriptorForValue() {
        return buildEntityDescriptor().getGenuineVariableDescriptor("value");
    }

    private TestdataValue value;
    private String firstShadow;

    public TestdataShadowedEntity() {
    }

    public TestdataShadowedEntity(String code) {
        super(code);
    }

    public TestdataShadowedEntity(String code, TestdataValue value) {
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

    @ShadowVariable(supplierName = "updateFirstShadow")
    public String getFirstShadow() {
        return firstShadow;
    }

    public void setFirstShadow(String firstShadow) {
        this.firstShadow = firstShadow;
    }

    // ************************************************************************
    // Complex methods
    // ************************************************************************

    @ShadowSources("value")
    public String updateFirstShadow() {
        if (value == null) {
            return null;
        }
        return value.getCode() + "/firstShadow";
    }

}
