package ai.timefold.solver.core.impl.testdata.domain.cascade.multiple_var;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.CascadingUpdateShadowVariable;
import ai.timefold.solver.core.api.domain.variable.InverseRelationShadowVariable;
import ai.timefold.solver.core.api.domain.variable.NextElementShadowVariable;
import ai.timefold.solver.core.api.domain.variable.PreviousElementShadowVariable;
import ai.timefold.solver.core.impl.domain.entity.descriptor.EntityDescriptor;

@PlanningEntity
public class TestdataMultipleCascadingValue {

    public static EntityDescriptor<TestdataMultipleCascadingSolution> buildEntityDescriptor() {
        return TestdataMultipleCascadingSolution.buildSolutionDescriptor()
                .findEntityDescriptorOrFail(TestdataMultipleCascadingValue.class);
    }

    @InverseRelationShadowVariable(sourceVariableName = "valueList")
    private TestdataMultipleCascadingEntity entity;
    @PreviousElementShadowVariable(sourceVariableName = "valueList")
    private TestdataMultipleCascadingValue previous;
    @NextElementShadowVariable(sourceVariableName = "valueList")
    private TestdataMultipleCascadingValue next;
    @CascadingUpdateShadowVariable(targetMethodName = "updateCascadeValue")
    private Integer cascadeValue;
    @CascadingUpdateShadowVariable(targetMethodName = "updateCascadeValue")
    private Integer secondCascadeValue;
    private Integer value;
    private int numberOfCalls = 0;

    public TestdataMultipleCascadingValue(Integer value) {
        this.value = value;
    }

    public TestdataMultipleCascadingEntity getEntity() {
        return entity;
    }

    public void setEntity(TestdataMultipleCascadingEntity entity) {
        this.entity = entity;
    }

    public TestdataMultipleCascadingValue getPrevious() {
        return previous;
    }

    public void setPrevious(TestdataMultipleCascadingValue previous) {
        this.previous = previous;
    }

    public TestdataMultipleCascadingValue getNext() {
        return next;
    }

    public void setNext(TestdataMultipleCascadingValue next) {
        this.next = next;
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

    public Integer getSecondCascadeValue() {
        return secondCascadeValue;
    }

    public void setSecondCascadeValue(Integer secondCascadeValue) {
        this.secondCascadeValue = secondCascadeValue;
    }

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
    public String toString() {
        return "TestdataMultipleCascadingValue{" +
                "value=" + value +
                '}';
    }
}
