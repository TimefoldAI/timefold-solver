package ai.timefold.solver.core.impl.testdata.domain.cascade.multiple_var.supply;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.CascadingUpdateShadowVariable;
import ai.timefold.solver.core.api.domain.variable.InverseRelationShadowVariable;
import ai.timefold.solver.core.api.domain.variable.PreviousElementShadowVariable;
import ai.timefold.solver.core.impl.domain.entity.descriptor.EntityDescriptor;
import ai.timefold.solver.core.impl.testdata.domain.cascade.multiple_var.TestdataMultipleCascadingBaseValue;

@PlanningEntity
public class TestdataMultipleCascadingWithSupplyValue
        implements TestdataMultipleCascadingBaseValue<TestdataMultipleCascadingWithSupplyEntity> {

    public static EntityDescriptor<TestdataMultipleCascadingWithSupplySolution> buildEntityDescriptor() {
        return TestdataMultipleCascadingWithSupplySolution.buildSolutionDescriptor()
                .findEntityDescriptorOrFail(TestdataMultipleCascadingWithSupplyValue.class);
    }

    @InverseRelationShadowVariable(sourceVariableName = "valueList")
    private TestdataMultipleCascadingWithSupplyEntity entity;
    @PreviousElementShadowVariable(sourceVariableName = "valueList")
    private TestdataMultipleCascadingWithSupplyValue previous;
    @CascadingUpdateShadowVariable(targetMethodName = "updateCascadeValue", sourceVariableName = "entity")
    @CascadingUpdateShadowVariable(targetMethodName = "updateCascadeValue", sourceVariableName = "previous")
    private Integer cascadeValue;
    @CascadingUpdateShadowVariable(targetMethodName = "updateCascadeValue", sourceVariableName = "entity")
    @CascadingUpdateShadowVariable(targetMethodName = "updateCascadeValue", sourceVariableName = "previous")
    private Integer secondCascadeValue;
    private Integer value;
    private int numberOfCalls = 0;

    public TestdataMultipleCascadingWithSupplyValue(Integer value) {
        this.value = value;
    }

    public TestdataMultipleCascadingWithSupplyEntity getEntity() {
        return entity;
    }

    @Override
    public void setEntity(TestdataMultipleCascadingWithSupplyEntity entity) {
        this.entity = entity;
    }

    public TestdataMultipleCascadingWithSupplyValue getPrevious() {
        return previous;
    }

    public void setPrevious(TestdataMultipleCascadingWithSupplyValue previous) {
        this.previous = previous;
    }

    @Override
    public Integer getValue() {
        return value;
    }

    public void setValue(Integer value) {
        this.value = value;
    }

    @Override
    public void setCascadeValue(Integer cascadeValue) {
        this.cascadeValue = cascadeValue;
    }

    @Override
    public Integer getCascadeValue() {
        return cascadeValue;
    }

    @Override
    public Integer getSecondCascadeValue() {
        return secondCascadeValue;
    }

    @Override
    public void setSecondCascadeValue(Integer secondCascadeValue) {
        this.secondCascadeValue = secondCascadeValue;
    }

    @Override
    public int getNumberOfCalls() {
        return numberOfCalls;
    }

    //---Complex methods---//
    public void updateCascadeValue() {
        numberOfCalls++;
        if (cascadeValue == null) {
            cascadeValue = value;
        }
        if (secondCascadeValue == null || secondCascadeValue != value + 1) {
            secondCascadeValue = value + 1;
        }
    }

    @Override
    public void reset() {
        numberOfCalls = 0;
    }

    @Override
    public String toString() {
        return "TestdataMultipleCascadeValue{" +
                "value=" + value +
                '}';
    }
}
