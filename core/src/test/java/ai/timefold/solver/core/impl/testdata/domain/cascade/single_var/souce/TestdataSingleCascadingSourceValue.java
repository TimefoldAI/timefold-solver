package ai.timefold.solver.core.impl.testdata.domain.cascade.single_var.souce;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.CascadingUpdateShadowVariable;
import ai.timefold.solver.core.impl.domain.entity.descriptor.EntityDescriptor;

@PlanningEntity
public class TestdataSingleCascadingSourceValue {

    public static EntityDescriptor<TestdataSingleCascadingSouceSolution> buildEntityDescriptor() {
        return TestdataSingleCascadingSouceSolution.buildSolutionDescriptor()
                .findEntityDescriptorOrFail(TestdataSingleCascadingSourceValue.class);
    }

    @CascadingUpdateShadowVariable(targetMethodName = "updateCascadeValue",
            sourceEntityClass = TestdataSingleCascadingSourceEntity.class, sourceVariableName = "valueList")
    private Integer cascadeValue;
    private Integer value;
    private int firstNumberOfCalls = 0;

    public TestdataSingleCascadingSourceValue(Integer value) {
        this.value = value;
    }

    public Integer getValue() {
        return value;
    }

    public void setValue(Integer value) {
        this.value = value;
    }

    public void setCascadeValue(Integer cascadeValue) {
        this.cascadeValue = cascadeValue;
    }

    public Integer getCascadeValue() {
        return cascadeValue;
    }

    public int getNumberOfCalls() {
        return firstNumberOfCalls;
    }

    //---Complex methods---//
    public void updateCascadeValue() {
        firstNumberOfCalls++;
        if (cascadeValue == null || cascadeValue != value + 1) {
            cascadeValue = value + 1;
        }
    }

    @Override
    public String toString() {
        return "TestdataSingleCascadingSourceValue{" +
                "value=" + value +
                '}';
    }
}
