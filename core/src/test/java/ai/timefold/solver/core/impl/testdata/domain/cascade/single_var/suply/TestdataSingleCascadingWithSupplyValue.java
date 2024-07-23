package ai.timefold.solver.core.impl.testdata.domain.cascade.single_var.suply;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.CascadingUpdateShadowVariable;
import ai.timefold.solver.core.api.domain.variable.InverseRelationShadowVariable;
import ai.timefold.solver.core.api.domain.variable.PreviousElementShadowVariable;
import ai.timefold.solver.core.impl.domain.entity.descriptor.EntityDescriptor;

@PlanningEntity
public class TestdataSingleCascadingWithSupplyValue {

    public static EntityDescriptor<TestdataSingleCascadingWithSupplySolution> buildEntityDescriptor() {
        return TestdataSingleCascadingWithSupplySolution.buildSolutionDescriptor()
                .findEntityDescriptorOrFail(TestdataSingleCascadingWithSupplyValue.class);
    }

    @InverseRelationShadowVariable(sourceVariableName = "valueList")
    private TestdataSingleCascadingWithSupplyEntity entity;
    @PreviousElementShadowVariable(sourceVariableName = "valueList")
    private TestdataSingleCascadingWithSupplyValue previous;
    @CascadingUpdateShadowVariable(targetMethodName = "updateCascadeValue", sourceVariableName = "entity")
    @CascadingUpdateShadowVariable(targetMethodName = "updateCascadeValue", sourceVariableName = "previous")
    private Integer cascadeValue;
    @CascadingUpdateShadowVariable(targetMethodName = "updateCascadeValueWithReturnType", sourceVariableName = "entity")
    @CascadingUpdateShadowVariable(targetMethodName = "updateCascadeValueWithReturnType", sourceVariableName = "previous")
    private Integer cascadeValueReturnType;
    private Integer value;
    private int firstNumberOfCalls = 0;
    private int secondNumberOfCalls = 0;

    public TestdataSingleCascadingWithSupplyValue(Integer value) {
        this.value = value;
    }

    public TestdataSingleCascadingWithSupplyEntity getEntity() {
        return entity;
    }

    public void setEntity(TestdataSingleCascadingWithSupplyEntity entity) {
        this.entity = entity;
    }

    public TestdataSingleCascadingWithSupplyValue getPrevious() {
        return previous;
    }

    public void setPrevious(TestdataSingleCascadingWithSupplyValue previous) {
        this.previous = previous;
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

    public Integer getCascadeValueReturnType() {
        return cascadeValueReturnType;
    }

    public int getFirstNumberOfCalls() {
        return firstNumberOfCalls;
    }

    public int getSecondNumberOfCalls() {
        return secondNumberOfCalls;
    }

    //---Complex methods---//
    public void updateCascadeValue() {
        firstNumberOfCalls++;
        if (cascadeValue == null || cascadeValue != value + 1) {
            cascadeValue = value + 1;
        }
    }

    public Integer updateCascadeValueWithReturnType() {
        secondNumberOfCalls++;
        if (cascadeValueReturnType == null || cascadeValueReturnType != value + 2) {
            cascadeValueReturnType = value + 2;
        }
        return cascadeValueReturnType;
    }

    public void reset() {
        firstNumberOfCalls = 0;
        secondNumberOfCalls = 0;
    }

    @Override
    public String toString() {
        return "TestdataCascadeValue{" +
                "value=" + value +
                '}';
    }
}
