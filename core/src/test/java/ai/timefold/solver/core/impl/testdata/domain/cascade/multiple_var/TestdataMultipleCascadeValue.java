package ai.timefold.solver.core.impl.testdata.domain.cascade.multiple_var;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.CascadeUpdateElementShadowVariable;
import ai.timefold.solver.core.api.domain.variable.InverseRelationShadowVariable;
import ai.timefold.solver.core.api.domain.variable.NextElementShadowVariable;
import ai.timefold.solver.core.api.domain.variable.PreviousElementShadowVariable;
import ai.timefold.solver.core.impl.domain.entity.descriptor.EntityDescriptor;

@PlanningEntity
public class TestdataMultipleCascadeValue {

    public static EntityDescriptor<TestdataMultipleCascadeSolution> buildEntityDescriptor() {
        return TestdataMultipleCascadeSolution.buildSolutionDescriptor()
                .findEntityDescriptorOrFail(TestdataMultipleCascadeValue.class);
    }

    @InverseRelationShadowVariable(sourceVariableName = "valueList")
    private TestdataMultipleCascadeEntity entity;
    @PreviousElementShadowVariable(sourceVariableName = "valueList")
    private TestdataMultipleCascadeValue previous;
    @NextElementShadowVariable(sourceVariableName = "valueList")
    private TestdataMultipleCascadeValue next;
    @CascadeUpdateElementShadowVariable(targetMethodName = "updateCascadeValue")
    private Integer cascadeValue;
    @CascadeUpdateElementShadowVariable(targetMethodName = "updateCascadeValue")
    private Integer secondCascadeValue;
    private Integer value;
    private int numberOfCalls = 0;

    public TestdataMultipleCascadeValue(Integer value) {
        this.value = value;
    }

    public TestdataMultipleCascadeEntity getEntity() {
        return entity;
    }

    public void setEntity(TestdataMultipleCascadeEntity entity) {
        this.entity = entity;
    }

    public TestdataMultipleCascadeValue getPrevious() {
        return previous;
    }

    public void setPrevious(TestdataMultipleCascadeValue previous) {
        this.previous = previous;
    }

    public TestdataMultipleCascadeValue getNext() {
        return next;
    }

    public void setNext(TestdataMultipleCascadeValue next) {
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
        return "TestdataMultipleCascadeValue{" +
                "value=" + value +
                '}';
    }
}
